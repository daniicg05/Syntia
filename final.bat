@echo off
cd /d C:\Users\USUARIO\IdeaProjects\syntia-mvp
git rm --cached push.bat 2>nul
del /f /q push.bat result.txt 2>nul
git add -A
git commit -m "chore: eliminar push.bat y result.txt" > nul 2>&1
git push origin preproduction > nul 2>&1

