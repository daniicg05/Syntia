@echo off
cd /d C:\Users\USUARIO\IdeaProjects\syntia-mvp
git add src\main\java\com\syntia\mvp\model\dto\ProyectoDTO.java
git add src\main\java\com\syntia\mvp\service\ProyectoService.java
git add src\main\java\com\syntia\mvp\controller\ProyectoController.java
git add src\main\resources\templates\usuario\proyectos\lista.html
git add src\main\resources\templates\usuario\proyectos\formulario.html
git add src\main\resources\templates\usuario\proyectos\detalle.html
git add src\main\resources\static\javascript\proyecto.js
git add src\main\resources\templates\usuario\dashboard.html
git commit -m "Fase 2: Gestion de proyectos - CRUD completo con ProyectoService, ProyectoController y vistas Thymeleaf" > C:\Users\USUARIO\IdeaProjects\syntia-mvp\push_result.txt 2>&1
git push origin preproduction >> C:\Users\USUARIO\IdeaProjects\syntia-mvp\push_result.txt 2>&1
echo DONE >> C:\Users\USUARIO\IdeaProjects\syntia-mvp\push_result.txt

