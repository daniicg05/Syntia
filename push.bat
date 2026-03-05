@echo off
cd /d C:\Users\USUARIO\IdeaProjects\syntia-mvp

rem Eliminar archivos temporales del disco
del /f /q gs.txt clean.bat clean_result.txt git_result.txt build2.ps1 build2.txt build3.ps1 build3.bat build3.txt run_build.ps1 sources.txt 2>nul

rem Sincronizar con git
git rm --cached fix.bat build2.ps1 build3.bat build3.ps1 run_build.ps1 clean.bat clean_result.txt git_result.txt gs.txt sources.txt 2>nul
git add -A
git status > C:\Users\USUARIO\IdeaProjects\syntia-mvp\result.txt 2>&1
git commit -m "chore: limpiar archivos temporales de build y sesion anterior" >> C:\Users\USUARIO\IdeaProjects\syntia-mvp\result.txt 2>&1
git push origin preproduction >> C:\Users\USUARIO\IdeaProjects\syntia-mvp\result.txt 2>&1

