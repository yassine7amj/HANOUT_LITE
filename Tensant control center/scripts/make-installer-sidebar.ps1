Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$outputPath = Join-Path $projectRoot "assets\installerSidebar.bmp"
$logoPath = Join-Path $projectRoot "assets\logo-128.png"

$bitmap = [System.Drawing.Bitmap]::new(164, 314)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit

$bounds = [System.Drawing.Rectangle]::new(0, 0, 164, 314)
$gradient = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
    $bounds,
    [System.Drawing.Color]::FromArgb(4, 16, 46),
    [System.Drawing.Color]::FromArgb(10, 36, 102),
    40
)
$graphics.FillRectangle($gradient, $bounds)

$halo = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(35, 43, 107, 236))
$graphics.FillEllipse($halo, 9, 6, 146, 146)
$logo = [System.Drawing.Image]::FromFile($logoPath)
$graphics.DrawImage($logo, 14, 12, 136, 136)

$gold = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(242, 193, 73))
$white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$muted = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(185, 201, 224))
$rule = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(196, 164, 86), 1)
$outline = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(205, 220, 243), 1)
$brandFont = [System.Drawing.Font]::new("Segoe UI", 7, [System.Drawing.FontStyle]::Bold)
$titleFont = [System.Drawing.Font]::new("Segoe UI", 10, [System.Drawing.FontStyle]::Bold)
$stepFont = [System.Drawing.Font]::new("Segoe UI", 8, [System.Drawing.FontStyle]::Regular)
$smallFont = [System.Drawing.Font]::new("Segoe UI", 6, [System.Drawing.FontStyle]::Regular)
$center = [System.Drawing.StringFormat]::new()
$center.Alignment = [System.Drawing.StringAlignment]::Center

$graphics.DrawString("TENSANTS GROUP", $brandFont, $gold, [System.Drawing.RectangleF]::new(4, 154, 156, 16), $center)
$graphics.DrawString("CONTROL CENTER", $titleFont, $white, [System.Drawing.RectangleF]::new(4, 172, 156, 22), $center)
$graphics.DrawLine($rule, 16, 202, 148, 202)

$steps = @(
    @{ Number = "1"; Label = "Welcome"; Active = $true },
    @{ Number = "2"; Label = "Install location"; Active = $false },
    @{ Number = "3"; Label = "Finish"; Active = $false }
)
$y = 209
foreach ($step in $steps) {
    if ($step.Active) {
        $active = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(242, 193, 73))
        $graphics.FillEllipse($active, 12, $y, 19, 19)
        $active.Dispose()
    } else {
        $graphics.DrawEllipse($outline, 12, $y, 19, 19)
    }
    $graphics.DrawString($step.Number, $stepFont, $white, [System.Drawing.RectangleF]::new(12, $y + 2, 19, 15), $center)
    $graphics.DrawString($step.Label, $stepFont, $white, 37, $y + 2)
    $y += 27
}

$graphics.DrawString("v0.1.0 · x64", $smallFont, $muted, 12, 289)
$graphics.DrawString("Designed & developed by FIPCOM Network", $smallFont, $muted, [System.Drawing.RectangleF]::new(12, 299, 145, 12))
$bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Bmp)

$center.Dispose()
$smallFont.Dispose()
$stepFont.Dispose()
$titleFont.Dispose()
$brandFont.Dispose()
$outline.Dispose()
$rule.Dispose()
$muted.Dispose()
$white.Dispose()
$gold.Dispose()
$logo.Dispose()
$halo.Dispose()
$gradient.Dispose()
$graphics.Dispose()
$bitmap.Dispose()

Write-Output "Created $outputPath"
