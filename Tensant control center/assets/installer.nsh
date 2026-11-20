!ifndef BUILD_UNINSTALLER
!include "nsDialogs.nsh"

Var CcInstallAllUsers
Var CcCreateDesktopShortcut
Var CcLaunchAfterInstall
Var CcAllUsersCheck
Var CcDesktopShortcutCheck
Var CcLaunchCheck
Var mui.FinishPage.Run
!define MUI_FINISHPAGE_RUN_VARIABLES

!macro customWelcomePage
  !define MUI_WELCOMEPAGE_TITLE "Welcome to the Control Center setup"
  !define MUI_WELCOMEPAGE_TEXT "This wizard will install TENSANTS GROUP CONTROL CENTER on your computer.$\r$\nIt is the central administration system for the TENSANTS GROUP ecosystem."
  !insertmacro MUI_PAGE_WELCOME

  Page custom CcOptionsPage CcOptionsLeave
!macroend

!macro customInstallmode
  ${if} $CcInstallAllUsers == "1"
    StrCpy $isForceMachineInstall "1"
  ${else}
    StrCpy $isForceCurrentInstall "1"
  ${endif}
!macroend

Function CcOptionsPage
  nsDialogs::Create 1018
  Pop $0
  ${If} $0 == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0u 0u 340u 24u "Choose setup options"
  Pop $0
  CreateFont $1 "Segoe UI" 11 700
  SendMessage $0 ${WM_SETFONT} $1 1

  ${NSD_CreateCheckbox} 0u 35u 340u 18u "Install for all users"
  Pop $CcAllUsersCheck
  ${NSD_CreateLabel} 18u 52u 322u 18u "Requires administrator rights"
  Pop $0

  ${NSD_CreateCheckbox} 0u 84u 340u 18u "Create a desktop shortcut"
  Pop $CcDesktopShortcutCheck
  ${NSD_CreateLabel} 18u 101u 322u 18u "Open the Control Center from your desktop"
  Pop $0

  ${NSD_CreateCheckbox} 0u 133u 340u 18u "Launch after installation"
  Pop $CcLaunchCheck
  ${NSD_CreateLabel} 18u 150u 322u 28u "Start the Control Center when setup finishes"
  Pop $0

  SendMessage $CcAllUsersCheck ${BM_SETCHECK} ${BST_CHECKED} 0
  SendMessage $CcDesktopShortcutCheck ${BM_SETCHECK} ${BST_CHECKED} 0
  nsDialogs::Show
FunctionEnd

Function CcOptionsLeave
  SendMessage $CcAllUsersCheck ${BM_GETCHECK} 0 0 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $CcInstallAllUsers "1"
  ${Else}
    StrCpy $CcInstallAllUsers "0"
  ${EndIf}

  SendMessage $CcDesktopShortcutCheck ${BM_GETCHECK} 0 0 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $CcCreateDesktopShortcut "1"
  ${Else}
    StrCpy $CcCreateDesktopShortcut "0"
  ${EndIf}

  SendMessage $CcLaunchCheck ${BM_GETCHECK} 0 0 $0
  ${If} $0 == ${BST_CHECKED}
    StrCpy $CcLaunchAfterInstall "1"
  ${Else}
    StrCpy $CcLaunchAfterInstall "0"
  ${EndIf}
FunctionEnd

!macro customInstall
  ${if} $CcCreateDesktopShortcut == "1"
    CreateShortCut "$newDesktopLink" "$appExe" "" "$appExe" 0 "" "" "${APP_DESCRIPTION}"
    ClearErrors
    WinShell::SetLnkAUMI "$newDesktopLink" "${APP_ID}"
  ${endif}
!macroend

!macro customFinishPage
  !define MUI_FINISHPAGE_TITLE "Setup completed successfully"
  !define MUI_FINISHPAGE_TEXT "TENSANTS GROUP CONTROL CENTER has been installed on your computer. You can start managing the ecosystem right away."
  !define MUI_FINISHPAGE_RUN
  !define MUI_FINISHPAGE_RUN_TEXT "Launch TENSANTS GROUP CONTROL CENTER"
  !define MUI_FINISHPAGE_RUN_FUNCTION "StartApp"
  !define MUI_FINISHPAGE_RUN_NOTCHECKED
  !define MUI_PAGE_CUSTOMFUNCTION_SHOW CcFinishPageShow

  Function CcFinishPageShow
    ${If} $CcLaunchAfterInstall == "1"
      SendMessage $mui.FinishPage.Run ${BM_SETCHECK} ${BST_CHECKED} 0
    ${Else}
      SendMessage $mui.FinishPage.Run ${BM_SETCHECK} ${BST_UNCHECKED} 0
    ${EndIf}
  FunctionEnd

  Function StartApp
    ${if} ${isUpdated}
      StrCpy $1 "--updated"
    ${else}
      StrCpy $1 ""
    ${endif}
    ${StdUtils.ExecShellAsUser} $0 "$launchLink" "open" "$1"
  FunctionEnd

  !insertmacro MUI_PAGE_FINISH
!macroend
!endif
