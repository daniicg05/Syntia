@echo off
cd /d C:\Users\USUARIO\IdeaProjects\syntia-mvp
git add src\main\java\com\syntia\mvp\repository\ConvocatoriaRepository.java
git add src\main\java\com\syntia\mvp\repository\RecomendacionRepository.java
git add src\main\java\com\syntia\mvp\model\dto\RecomendacionDTO.java
git add src\main\java\com\syntia\mvp\service\MotorMatchingService.java
git add src\main\java\com\syntia\mvp\service\RecomendacionService.java
git add src\main\java\com\syntia\mvp\controller\RecomendacionController.java
git add src\main\resources\templates\usuario\proyectos\recomendaciones.html
git add src\main\resources\templates\usuario\proyectos\detalle.html
git commit -m "Fase 3: Convocatorias + Motor de matching (rule-based) + Recomendaciones priorizadas con filtros y aviso legal" > result_f3.txt 2>&1
git push origin preproduction >> result_f3.txt 2>&1
echo DONE >> result_f3.txt

