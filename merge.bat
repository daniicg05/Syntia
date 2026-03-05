@echo off
cd /d C:\Users\USUARIO\IdeaProjects\syntia-mvp
set LOG=C:\Users\USUARIO\IdeaProjects\syntia-mvp\merge_result.txt

git checkout --ours .gitignore >> %LOG% 2>&1
git checkout --ours docs/01-requisitos.md >> %LOG% 2>&1
git checkout --ours docs/02-plan-proyecto.md >> %LOG% 2>&1
git checkout --ours docs/03-especificaciones-tecnicas.md >> %LOG% 2>&1
git checkout --ours docs/04-manual-desarrollo.md >> %LOG% 2>&1
git checkout --ours docs/05-changelog.md >> %LOG% 2>&1
git checkout --ours docs/06-diagramas.md >> %LOG% 2>&1
git checkout --ours pom.xml >> %LOG% 2>&1
git checkout --ours src/main/java/com/syntia/mvp/SyntiaMvpApplication.java >> %LOG% 2>&1
git checkout --ours src/main/resources/application.properties >> %LOG% 2>&1

git add . >> %LOG% 2>&1
git commit -m "merge: resolver conflictos manteniendo version preproduction" >> %LOG% 2>&1
git push origin preproduction >> %LOG% 2>&1
echo DONE >> %LOG%

