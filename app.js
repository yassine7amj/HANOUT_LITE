/* HANOUT LITE - shared application logic (used by index.html mobile + web.html desktop) */
const KEY='hanout_ai_lite_v01';let pendingImage=null;let catList=[];
const defaults={name:'حانوت الحومة — متجر تجريبي',phone:'',products:[{id:'p1',name:'حليب 1 لتر',price:8,oldPrice:10,category:'حليب ومشتقاته',emoji:'🥛',available:true},{id:'p2',name:'خبز',price:1.5,category:'مخبوزات',emoji:'🥖',available:true},{id:'p3',name:'سكر 1 كيلو',price:7,oldPrice:9,category:'مواد غذائية',emoji:'🍬',available:true},{id:'p4',name:'زيت 1 لتر',price:19,category:'مواد غذائية',emoji:'🫒',available:true},{id:'p5',name:'ماء 1.5 لتر',price:6,category:'مشروبات',emoji:'💧',available:true},{id:'p6',name:'قهوة',price:15,oldPrice:18,category:'مشروبات',emoji:'☕',available:true}]};
let data;try{let raw=JSON.parse(localStorage.getItem(KEY));data=validData(raw)?raw:structuredClone(defaults)}catch(e){data=structuredClone(defaults)}
function normalize(){if(!Array.isArray(data.categories))data.categories=[...new Set(data.products.map(p=>p.category))].sort().map(c=>({name:c,emoji:(data.products.find(p=>p.category===c)||{}).emoji||'🛍️'}));if(!Array.isArray(data.orders))data.orders=[];if(!Array.isArray(data.favs))data.favs=[]}
normalize();
let cart={};
/* ===== منصة متعددة التجار (Multi-tenant) ===== */
const PARAMS=new URLSearchParams(location.search);
const SHOP_PARAM=PARAMS.get('shop');
const IS_MERCHANT_DASH=!!document.getElementById('manage');
let shopSlug=SHOP_PARAM||null;     // slug للقراءة (واجهة الزبون)
let cloudShop=null;                // slug للكتابة (لوحة التاجر)
let cloudReady=false;let cloudTimer=null;
const el=id=>document.getElementById(id);
const money=n=>Number(n).toFixed(2)+' DH';
const escapeHTML=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function validData(d){return !!(d&&typeof d.name==='string'&&typeof d.phone==='string'&&Array.isArray(d.products)&&d.products.length<=1000&&d.products.every(p=>typeof p.id==='string'&&typeof p.name==='string'&&p.name.length<=70&&Number.isFinite(p.price)&&p.price>=0&&p.price<=100000&&typeof p.category==='string'&&typeof p.emoji==='string'&&typeof p.available==='boolean'&&(p.image===undefined||typeof p.image==='string'&&p.image.length<180000)&&(p.oldPrice===undefined||p.oldPrice===null||Number.isFinite(p.oldPrice)&&p.oldPrice>=0&&p.oldPrice<=100000)))}
function persist(){try{localStorage.setItem(KEY,JSON.stringify(data))}catch(e){toast('مساحة التخزين ممتلئة. احذف بعض الصور أو صدّر نسخة احتياطية.');return false}if(cloudShop)scheduleCloudSave();return true}
function scheduleCloudSave(){if(!window.HanoutStore)return;clearTimeout(cloudTimer);cloudTimer=setTimeout(()=>{HanoutStore.saveCatalog(cloudShop,{products:data.products,categories:data.categories}).catch(()=>{});HanoutStore.saveMerchant({slug:cloudShop,name:data.name,phone:data.phone}).catch(()=>{})},600)}
function toast(t,icon){el('toast').innerHTML=(icon?`<i class="fa-solid ${icon}"></i>`:'')+escapeHTML(t);el('toast').style.display='block';clearTimeout(toast._t);toast._t=setTimeout(()=>el('toast').style.display='none',2800)}

function showTab(tab,scroll=true){['customer','merchant','myorders','about'].forEach(t=>{const s=el(t);if(!s)return;s.classList.toggle('hidden',t!==tab);const b=el('nav'+t.charAt(0).toUpperCase()+t.slice(1));if(b)b.classList.toggle('active',t===tab)});if(scroll)window.scrollTo({top:0,behavior:'smooth'});render()}
function goDeals(){const d=el('deals');if(!d)return;showTab('customer',false);d.scrollIntoView({behavior:'smooth',block:'start'})}
function goCart(){const c=el('cartPanel');if(!c)return;showTab('customer',false);c.scrollIntoView({behavior:'smooth',block:'start'})}

function openMenu(){const d=el('drawer');if(!d)return;const s=el('drawerShop');if(s)s.textContent=data.name;d.classList.add('open');d.setAttribute('aria-hidden','false');document.body.style.overflow='hidden'}
function closeMenu(){const d=el('drawer');if(!d)return;d.classList.remove('open');d.setAttribute('aria-hidden','true');document.body.style.overflow=''}
function menuGo(tab,after){closeMenu();setTimeout(()=>{if(tab)showTab(tab,false);if(typeof after==='function')after()},180)}
document.addEventListener('keydown',e=>{if(e.key==='Escape')closeMenu()});
function handleHash(){const h=location.hash;if(h==='#deals')goDeals();else if(h==='#cart')goCart()}

function render(){const cats=[...new Set(data.products.map(p=>p.category))].sort();
  if(el('shopTitle'))el('shopTitle').textContent=data.name;
  if(el('shopName'))el('shopName').value=data.name;
  if(el('shopPhone'))el('shopPhone').value=data.phone;
  if(el('category')){let prev=el('category').value;
    el('category').innerHTML='<option value="">جميع الأصناف</option>'+cats.map(c=>`<option value="${escapeHTML(c)}">${escapeHTML(c)}</option>`).join('');
    el('category').value=cats.includes(prev)?prev:''}
  if(el('catOptions'))el('catOptions').innerHTML=cats.map(c=>`<option value="${escapeHTML(c)}"></option>`).join('');
  if(el('cats'))renderCats();
  if(el('deals'))renderDeals();
  if(el('products'))renderProducts();
  if(el('cart'))renderCart();
  if(el('myOrders'))renderMyOrders();
  if(el('manage'))renderManage();
  if(el('orders'))renderOrders();
  if(el('catsManage'))renderCatsManage()}

function renderCats(){catList=[...new Set(data.products.map(p=>p.category))].sort();const active=el('category').value;
  el('cats').innerHTML=`<button class="cat ${!active?'active':''}" onclick="pickCat(-1)"><span class="cat-ic"><i class="fa-solid fa-basket-shopping"></i></span><span>الكل</span></button>`+
  catList.map((c,i)=>{const p=data.products.find(x=>x.category===c);return `<button class="cat ${active===c?'active':''}" onclick="pickCat(${i})"><span class="cat-ic">${p?escapeHTML(p.emoji||'🛍️'):'🛍️'}</span><span>${escapeHTML(c)}</span></button>`}).join('')}
function pickCat(i){el('category').value=i<0?'':catList[i];renderProducts();renderCats();el('products').scrollIntoView({behavior:'smooth',block:'start'})}

function cardMarkup(p,cls){const old=Number(p.oldPrice||0),disc=old>p.price,up=old>0&&old<p.price,pid=p.id.replace(/[^a-zA-Z0-9_-]/g,''),fav=isFav(pid);
  const badge=disc?`<span class="deal-badge"><i class="fa-solid fa-tag"></i> -${Math.round((old-p.price)/old*100)}%</span>`:(up?`<span class="up-badge"><i class="fa-solid fa-arrow-trend-up"></i> زيادة</span>`:'');
  const favBtn=`<button class="favbtn ${fav?'on':''}" onclick="toggleFav('${pid}')" aria-label="المفضلة" aria-pressed="${fav}"><i class="fa-${fav?'solid':'regular'} fa-heart"></i></button>`;
  const btn=p.available?`<button class="btn-add" onclick="addCart('${pid}')"><i class="fa-solid fa-cart-plus"></i> زيد للسلة</button>`:'<button class="btn-add" disabled><i class="fa-solid fa-ban"></i> غير متوفر</button>';
  return `<article class="${cls}">${badge}${favBtn}<div class="pimg">${p.image?`<img src="${p.image}" alt="${escapeHTML(p.name)}">`:`<span class="pemoji">${escapeHTML(p.emoji||'🛍️')}</span>`}</div><span class="muted">${escapeHTML(p.category)}</span><h3>${escapeHTML(p.name)}</h3><div class="pricebox"><span class="price">${money(p.price)}</span>${disc?`<span class="oldprice">${money(old)}</span>`:''}</div>${btn}</article>`}

function renderProducts(){let q=el('search').value.trim().toLowerCase(),cat=el('category').value;
  let items=data.products.filter(p=>(!q||p.name.toLowerCase().includes(q))&&(!cat||p.category===cat));
  if(el('onlyAvailable').checked)items=items.filter(p=>p.available);
  if(el('onlyFavs').checked)items=items.filter(p=>isFav(p.id));
  const s=el('sortBy').value;
  if(s==='price-asc')items=[...items].sort((a,b)=>a.price-b.price);
  else if(s==='price-desc')items=[...items].sort((a,b)=>b.price-a.price);
  else if(s==='name')items=[...items].sort((a,b)=>a.name.localeCompare(b.name,'ar'));
  else if(s==='new')items=[...items].reverse();
  el('products').innerHTML=items.length?items.map(p=>cardMarkup(p,'product')).join(''):`<div class="empty"><i class="fa-solid fa-magnifying-glass"></i>${el('onlyFavs').checked?'ما كاين حتى منتوج فالمفضلة دابا.':'ما لقينا حتى منتوج.'}</div>`}

function isFav(id){return data.favs.includes(id)}
function toggleFav(id){if(isFav(id))data.favs=data.favs.filter(x=>x!==id);else data.favs.push(id);persist();renderProducts();renderDeals();toast(isFav(id)?'تزاد للمفضلة':'تحيد من المفضلة','fa-heart')}

function renderMyOrders(){const list=data.orders;
  el('myOrders').innerHTML=list.length?list.map(o=>{const t=new Date(o.ts).toLocaleString('ar-MA',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}),done=o.status!=='جديد';
    return `<div class="orderRow"><div class="otop"><b>${money(o.total)}</b><span class="chip ${done?'done':'new'}">${done?'تم التأكيد':'قيد الانتظار'}</span></div><div class="muted" style="font-size:12px">${t}${o.fulfillment?' • '+escapeHTML(o.fulfillment):''}</div><ul>${o.items.map(i=>`<li>${escapeHTML(i.name)} × ${i.qty} = ${money(i.price*i.qty)}</li>`).join('')}</ul></div>`}).join(''):'<p class="empty"><i class="fa-solid fa-receipt"></i>مازال ما صيفطتي حتى طلب من هاد الجهاز.</p>'}

function renderDeals(){const deals=data.products.filter(p=>Number(p.oldPrice||0)>p.price).sort((a,b)=>(b.oldPrice-b.price)-(a.oldPrice-a.price)).slice(0,10);
  el('deals').innerHTML=deals.length?deals.map(p=>cardMarkup(p,'deal')).join(''):'<div class="empty"><i class="fa-solid fa-tag"></i>مازال ما كاين حتى تخفيض.</div>'}

function addCart(id){let p=data.products.find(x=>x.id===id);if(!p||!p.available)return;cart[id]=(cart[id]||0)+1;renderCart();toast('تزاد المنتوج للسلة','fa-circle-check')}
function updateQty(id,n){cart[id]=Math.max(0,(cart[id]||0)+n);if(!cart[id])delete cart[id];renderCart()}
function renderCart(){let entries=Object.entries(cart).map(([id,qty])=>({p:data.products.find(p=>p.id===id),qty,id})).filter(x=>x.p&&x.p.available);
  let total=entries.reduce((a,x)=>a+x.p.price*x.qty,0);
  el('cart').innerHTML=entries.length?entries.map(x=>`<div class="cartrow"><div><b>${escapeHTML(x.p.name)}</b><div class="muted">${money(x.p.price*x.qty)}</div></div><div class="actions"><button class="mini" onclick="updateQty('${x.id}',-1)"><i class="fa-solid fa-minus"></i></button><b>${x.qty}</b><button class="mini" onclick="updateQty('${x.id}',1)"><i class="fa-solid fa-plus"></i></button></div></div>`).join('')+`<div class="cartrow"><b>المجموع التقديري</b><b>${money(total)}</b></div>`:'<div class="empty"><i class="fa-solid fa-basket-shopping"></i>السلة خاوية دابا</div>';
  const count=entries.reduce((a,x)=>a+x.qty,0);const sc=el('statCart');if(sc)sc.textContent=count;const cb=el('cartBadge');if(cb)cb.textContent=count}

function checkout(){let entries=Object.entries(cart).map(([id,qty])=>({p:data.products.find(p=>p.id===id),qty})).filter(x=>x.p&&x.p.available);
  if(!entries.length)return toast('زيد شي منتوج للسلة أولاً','fa-triangle-exclamation');
  let phone=data.phone.replace(/\D/g,'');if(!/^212[5-7]\d{8}$/.test(phone))return toast('خاص مول الحانوت يدخل رقم واتساب صحيح','fa-triangle-exclamation');
  let total=entries.reduce((a,x)=>a+x.p.price*x.qty,0);
  let lines=[`السلام عليكم، بغيت نطلب من ${data.name}:`,...entries.map(x=>`• ${x.p.name} × ${x.qty} = ${money(x.p.price*x.qty)}`),`المجموع التقديري: ${money(total)}`,`الاستلام: ${el('fulfillment').value}`,`الاسم: ${el('buyer').value.trim()||'غير محدد'}`,`ملاحظة: ${el('note').value.trim()||'لا توجد'}`,'المرجو تأكيد التوفر والثمن النهائي.'];
  data.orders.unshift({id:'o'+Date.now().toString(36),ts:Date.now(),buyer:el('buyer').value.trim()||'غير محدد',fulfillment:el('fulfillment').value,note:el('note').value.trim(),items:entries.map(x=>({name:x.p.name,qty:x.qty,price:x.p.price})),total,status:'جديد'});
  if(data.orders.length>200)data.orders.length=200;persist();renderOrders();
  if(shopSlug&&window.HanoutStore){const o=data.orders[0];HanoutStore.addOrder(shopSlug,{id:o.id,ts:o.ts,buyer:o.buyer,fulfillment:o.fulfillment,note:o.note,items:o.items,total:o.total,status:'جديد'}).catch(()=>{})}
  window.open('https://wa.me/'+phone+'?text='+encodeURIComponent(lines.join('\n')),'_blank','noopener,noreferrer')}

function renderManage(){el('statProducts').textContent=data.products.length;el('statAvailable').textContent=data.products.filter(p=>p.available).length;
  const q=(el('manageSearch')?el('manageSearch').value:'').trim().toLowerCase(),sort=el('manageSort')?el('manageSort').value:'recent';
  let items=data.products.filter(p=>!q||p.name.toLowerCase().includes(q)||p.category.toLowerCase().includes(q));
  if(sort==='name')items=[...items].sort((a,b)=>a.name.localeCompare(b.name,'ar'));else if(sort==='price-asc')items=[...items].sort((a,b)=>a.price-b.price);else if(sort==='price-desc')items=[...items].sort((a,b)=>b.price-a.price);
  el('manage').innerHTML=items.length?items.map(p=>`<div class="manageRow"><div style="display:flex;gap:9px;align-items:center">${p.image?`<img src="${p.image}" alt="" style="width:54px;height:54px;object-fit:cover;border-radius:9px">`:`<span style="font-size:26px">${escapeHTML(p.emoji)}</span>`}<div><b>${escapeHTML(p.name)}</b><div class="muted">${money(p.price)} • ${p.available?'متوفر':'غير متوفر'}</div>${p.oldPrice&&p.oldPrice!==p.price?`<div class="muted">الثمن السابق: ${money(p.oldPrice)}</div>`:''}</div></div><div class="actions"><button class="mini" onclick="editProduct('${p.id}')"><i class="fa-solid fa-pen"></i> تعديل</button><button class="mini danger" onclick="deleteProduct('${p.id}')"><i class="fa-solid fa-trash"></i> حذف</button></div></div>`).join(''):'<p class="empty"><i class="fa-solid fa-box-open"></i>ما لقينا حتى منتوج.</p>'}

function previewImage(e){let file=e.target.files[0];if(!file)return;if(!['image/jpeg','image/png','image/webp'].includes(file.type)||file.size>5*1024*1024){toast('اختر صورة JPG أو PNG أو WebP أقل من 5MB','fa-triangle-exclamation');e.target.value='';return}let img=new Image(),url=URL.createObjectURL(file);img.onload=()=>{let scale=Math.min(1,650/img.width,650/img.height),canvas=document.createElement('canvas');canvas.width=Math.max(1,Math.round(img.width*scale));canvas.height=Math.max(1,Math.round(img.height*scale));canvas.getContext('2d').drawImage(img,0,0,canvas.width,canvas.height);pendingImage=canvas.toDataURL('image/jpeg',0.68);el('imagePreview').src=pendingImage;el('imagePreview').classList.remove('hidden');URL.revokeObjectURL(url)};img.onerror=()=>{URL.revokeObjectURL(url);toast('تعذر قراءة الصورة','fa-triangle-exclamation')};img.src=url}
function removeImage(){pendingImage='';el('imagePreview').removeAttribute('src');el('imagePreview').classList.add('hidden');el('pImage').value=''}
function saveProduct(e){e.preventDefault();let id=el('editId').value||'p'+Date.now().toString(36),existing=data.products.find(x=>x.id===id);let oldText=el('pOldPrice').value;
  let p={id,name:el('pName').value.trim(),price:Number(el('pPrice').value),oldPrice:oldText===''?null:Number(oldText),category:el('pCategory').value.trim(),emoji:el('pEmoji').value.trim()||'🛍️',available:el('pAvailable').checked,image:pendingImage===null?(existing?.image||''):pendingImage};
  if(!p.name||!p.category||!Number.isFinite(p.price)||p.price<0||p.price>100000||p.oldPrice!==null&&(!Number.isFinite(p.oldPrice)||p.oldPrice<0||p.oldPrice>100000))return toast('راجع معلومات المنتوج','fa-triangle-exclamation');
  if(!data.categories.some(c=>c.name===p.category))data.categories.push({name:p.category,emoji:p.emoji});
  let snapshot=JSON.stringify(data),idx=data.products.findIndex(x=>x.id===id);if(idx<0)data.products.push(p);else data.products[idx]=p;if(!persist()){data=JSON.parse(snapshot);return}resetForm();render();toast('تحفظ المنتوج بنجاح','fa-circle-check')}
function editProduct(id){let p=data.products.find(x=>x.id===id);if(!p)return;el('editId').value=p.id;el('pName').value=p.name;el('pPrice').value=p.price;el('pOldPrice').value=p.oldPrice??'';el('pCategory').value=p.category;el('pEmoji').value=p.emoji;el('pAvailable').checked=p.available;pendingImage=p.image||'';if(p.image){el('imagePreview').src=p.image;el('imagePreview').classList.remove('hidden')}else{el('imagePreview').classList.add('hidden')}el('formTitle').innerHTML='<i class="fa-solid fa-pen"></i>تعديل المنتج';el('productForm').scrollIntoView({behavior:'smooth',block:'center'})}
function deleteProduct(id){if(!confirm('واش متأكد بغيتي تحذف المنتوج؟'))return;data.products=data.products.filter(p=>p.id!==id);delete cart[id];persist();render()}
function resetForm(){el('productForm').reset();el('editId').value='';pendingImage=null;el('imagePreview').classList.add('hidden');el('imagePreview').removeAttribute('src');el('formTitle').innerHTML='<i class="fa-solid fa-plus"></i>إضافة منتج'}
function saveSettings(){let name=el('shopName').value.trim(),phone=el('shopPhone').value.replace(/\D/g,'');if(!name)return toast('دخل اسم الحانوت','fa-triangle-exclamation');if(phone&&!/^212[5-7]\d{8}$/.test(phone))return toast('رقم واتساب غير صحيح. مثال: 212612345678','fa-triangle-exclamation');data.name=name;data.phone=phone;persist();render();toast('تحفظات الإعدادات','fa-circle-check')}
function exportData(){let blob=new Blob([JSON.stringify(data,null,2)],{type:'application/json'}),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download='hanout-catalog.json';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}
async function importData(e){let f=e.target.files[0];if(!f)return;try{if(f.size>250000)throw Error('الملف كبير');let d=JSON.parse(await f.text());if(!validData(d))throw Error('صيغة الملف غير صحيحة');if(!confirm('غادي يتبدل الكتالوغ الحالي. واش موافق؟'))return;data=d;normalize();cart={};persist();render();toast('تم الاستيراد','fa-circle-check')}catch(err){toast(err.message,'fa-triangle-exclamation')}finally{e.target.value=''}}
function renderOrders(){const list=data.orders;const so=el('statOrders');if(so)so.textContent=list.filter(o=>o.status==='جديد').length;
  const box=el('orders');if(!box)return;
  box.innerHTML=list.length?list.map(o=>{const t=new Date(o.ts).toLocaleString('ar-MA',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'});
    return `<div class="orderRow"><div class="otop"><b>${escapeHTML(o.buyer)} • ${money(o.total)}</b><span class="chip ${o.status==='جديد'?'new':'done'}">${o.status==='جديد'?'جديد':'تم'}</span></div><div class="muted" style="font-size:12px">${t}${o.fulfillment?' • '+escapeHTML(o.fulfillment):''}</div><ul>${o.items.map(i=>`<li>${escapeHTML(i.name)} × ${i.qty} = ${money(i.price*i.qty)}</li>`).join('')}</ul>${o.note?`<div class="muted" style="font-size:12px">ملاحظة: ${escapeHTML(o.note)}</div>`:''}<div class="actions" style="margin-top:8px"><button class="mini" onclick="setOrderStatus('${o.id}')"><i class="fa-solid fa-check"></i> ${o.status==='جديد'?'تم':'رجع جديد'}</button><button class="mini danger" onclick="deleteOrder('${o.id}')"><i class="fa-solid fa-trash"></i> حذف</button></div></div>`}).join(''):'<p class="empty"><i class="fa-solid fa-receipt"></i>مازال ما كاين حتى طلب.</p>'}
function setOrderStatus(id){let o=data.orders.find(x=>x.id===id);if(!o)return;o.status=o.status==='جديد'?'تم':'جديد';if(cloudShop&&window.HanoutStore)HanoutStore.updateOrder(cloudShop,id,{status:o.status}).catch(()=>{});renderOrders()}
function deleteOrder(id){if(!confirm('تحذف هاد الطلب؟'))return;data.orders=data.orders.filter(x=>x.id!==id);if(cloudShop&&window.HanoutStore)HanoutStore.deleteOrder(cloudShop,id).catch(()=>{});renderOrders()}

function renderCatsManage(){const counts={};data.products.forEach(p=>counts[p.category]=(counts[p.category]||0)+1);
  el('catsManage').innerHTML=data.categories.length?data.categories.map((c,i)=>`<div class="catRow"><div class="cname"><span class="ce">${escapeHTML(c.emoji||'🛍️')}</span><div>${escapeHTML(c.name)}<div class="muted" style="font-size:12px">${counts[c.name]||0} منتوج</div></div></div><div class="actions"><button class="mini" onclick="renameCategory(${i})"><i class="fa-solid fa-pen"></i> تعديل</button><button class="mini danger" onclick="deleteCategory(${i})"><i class="fa-solid fa-trash"></i> حذف</button></div></div>`).join(''):'<p class="empty"><i class="fa-solid fa-layer-group"></i>مازال ما كاين حتى صنف.</p>'}
function addCategory(){let name=el('newCatName').value.trim(),emoji=el('newCatEmoji').value.trim()||'🛍️';if(!name)return toast('دخل اسم الصنف','fa-triangle-exclamation');if(data.categories.some(c=>c.name===name))return toast('هاد الصنف كاين من قبل','fa-triangle-exclamation');data.categories.push({name,emoji});el('newCatName').value='';el('newCatEmoji').value='';persist();render();toast('تزاد الصنف','fa-circle-check')}
function renameCategory(i){let c=data.categories[i];if(!c)return;let old=c.name,name=prompt('الاسم الجديد للصنف:',old);if(name===null)return;name=name.trim();if(!name)return;if(name!==old&&data.categories.some(x=>x.name===name))return toast('هاد الاسم مستعمل من قبل','fa-triangle-exclamation');let emoji=prompt('الرمز التعبيري للصنف:',c.emoji||'🛍️');c.name=name;c.emoji=((emoji||'').trim())||'🛍️';if(name!==old)data.products.forEach(p=>{if(p.category===old)p.category=name});persist();render();toast('تبدل الصنف','fa-circle-check')}
function deleteCategory(i){let c=data.categories[i];if(!c)return;let name=c.name,used=data.products.filter(p=>p.category===name).length;if(used&&!confirm('فيه '+used+' منتوج فهاد الصنف. غادي ينقلو لصنف «غير مصنف». موافق؟'))return;if(used){if(!data.categories.some(x=>x.name==='غير مصنف'))data.categories.push({name:'غير مصنف',emoji:'📦'});data.products.forEach(p=>{if(p.category===name)p.category='غير مصنف'})}data.categories=data.categories.filter(x=>x.name!==name);persist();render();toast('تحذف الصنف','fa-circle-check')}

async function shareApp(){const url=location.href.split('#')[0];const text='HANOUT LITE — '+(data.name||'')+'\n'+url;try{if(navigator.share){await navigator.share({title:'HANOUT LITE',text:'HANOUT LITE — '+(data.name||''),url});return}await navigator.clipboard.writeText(url);toast('تنسخ رابط التطبيق','fa-circle-check')}catch(err){window.open('https://wa.me/?text='+encodeURIComponent(text),'_blank','noopener,noreferrer')}}

let deferredPrompt=null;
window.addEventListener('beforeinstallprompt',e=>{e.preventDefault();deferredPrompt=e;const b=el('installCta');if(b)b.style.display='flex'});
window.addEventListener('appinstalled',()=>{deferredPrompt=null;const b=el('installCta');if(b)b.style.display='none';toast('تثبت التطبيق بنجاح','fa-circle-check')});
async function installApp(){const b=el('installCta');if(deferredPrompt){deferredPrompt.prompt();try{await deferredPrompt.userChoice}catch(err){}deferredPrompt=null;if(b)b.style.display='none';return}if(b)b.style.display='none';toast('من قائمة المتصفح: «إضافة إلى الشاشة الرئيسية»','fa-circle-info')}
const THEME_KEY='hanout_theme';
function applyTheme(){const dark=localStorage.getItem(THEME_KEY)==='dark';document.documentElement.setAttribute('data-theme',dark?'dark':'light');const i=el('themeIcon');if(i)i.className=dark?'fa-solid fa-sun':'fa-solid fa-moon';const b=el('themeBtn');if(b)b.setAttribute('aria-label',dark?'الوضع النهاري':'الوضع الليلي')}
function toggleTheme(){try{localStorage.setItem(THEME_KEY,document.documentElement.getAttribute('data-theme')==='dark'?'light':'dark')}catch(e){}applyTheme()}
if('serviceWorker' in navigator){window.addEventListener('load',()=>{navigator.serviceWorker.register('./service-worker.js').catch(()=>{})})}
/* ===== تشغيل السحابة: تعدد التجار ===== */
function applyShop(shop){if(!shop)return;
  if(shop.name)data.name=shop.name;
  if(typeof shop.phone==='string'&&shop.phone)data.phone=shop.phone;
  if(Array.isArray(shop.products))data.products=shop.products;
  if(Array.isArray(shop.categories)&&shop.categories.length)data.categories=shop.categories;
  else data.categories=[...new Set(data.products.map(p=>p.category))].sort().map(c=>({name:c,emoji:(data.products.find(p=>p.category===c)||{}).emoji||'🛍️'}));
  if(shop.color)document.documentElement.style.setProperty('--blue',shop.color);
  document.title=data.name+' | HANOUT LITE'}
function blocked(t,m){document.body.innerHTML='<div style="min-height:100vh;display:grid;place-items:center;padding:30px;text-align:center;font-family:system-ui;background:#F5F8FD"><div><div style="font-size:52px">🚧</div><h2 style="color:#0B1B34">'+escapeHTML(t)+'</h2><p style="color:#5B6B82;max-width:44ch">'+escapeHTML(m)+'</p></div></div>'}
async function cloudBoot(){
  if(!window.HanoutStore)return;
  cloudReady=await HanoutStore.ready();
  document.documentElement.setAttribute('data-backend',cloudReady?'cloud':'local');
  if(IS_MERCHANT_DASH){
    const u=await HanoutStore.waitAuth();
    if(!u){location.replace('login.html?next=merchant');return}
    const prof=await HanoutStore.getProfile();
    if(!prof||prof.role==='admin'||!prof.slug){location.replace('login.html?next=merchant');return}
    cloudShop=prof.slug;
    const shop=await HanoutStore.loadShop(cloudShop);
    if(shop&&shop.active===false){blocked('الحانوت موقوف','هاد الحانوت توقف مؤقتاً من طرف إدارة المنصة. المرجو التواصل مع الإدارة.');return}
    if(shop)applyShop(shop);
    data.orders=await HanoutStore.listOrders(cloudShop);
    window.HANOUT_SHOP=shop;
    render();
    setTimeout(()=>{if(typeof window.onShopReady==='function')try{window.onShopReady(shop)}catch(e){}},0);
  }else if(SHOP_PARAM){
    const shop=await HanoutStore.loadShop(SHOP_PARAM);
    if(!shop){toast('هاد الحانوت ما كاينش','fa-triangle-exclamation');return}
    if(shop.active===false){blocked('الحانوت موقوف','هاد الحانوت توقف مؤقتاً. المرجو المحاولة لاحقاً.');return}
    shopSlug=SHOP_PARAM;applyShop(shop);render();handleHash();
  }else if(cloudReady){
    // بلا ?shop= : كنعرضو أول حانوت نشط فالمنصة (التطبيق العام)
    const slug=await HanoutStore.firstActiveSlug();
    if(!slug)return;
    const shop=await HanoutStore.loadShop(slug);
    if(!shop||shop.active===false)return;
    shopSlug=slug;applyShop(shop);render();handleHash();
  }
}
cloudBoot();
window.addEventListener('hashchange',handleHash);

applyTheme();
render();
handleHash();
