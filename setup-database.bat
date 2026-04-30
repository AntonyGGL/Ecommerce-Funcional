@echo off
REM Script para crear la base de datos PostgreSQL para Impofer (Windows)

echo Creando base de datos PostgreSQL para Impofer...
echo.

REM Buscar psql en las rutas comunes
where psql >nul 2>nul
if %errorlevel% neq 0 (
    echo PostgreSQL no se encontro en la ruta del sistema.
    echo Por favor, agrega PostgreSQL bin folder a tu PATH o ejecuta desde:
    echo "C:\Program Files\PostgreSQL\[version]\bin\psql.exe"
    pause
    exit /b 1
)

echo Conectando a PostgreSQL como usuario 'postgres'...
psql -U postgres -c "CREATE DATABASE impofer_db ENCODING 'UTF8';"

if %errorlevel% equ 0 (
    echo.
    echo ✓ Base de datos 'impofer_db' creada exitosamente
) else (
    echo.
    echo ✗ Error al crear la base de datos
    echo   (Puede que ya exista o las credenciales sean incorrectas)
)

echo.
echo Verificando la conexion a la base de datos...
psql -U postgres -d impofer_db -c "SELECT 'Conexion exitosa' as status;"

if %errorlevel% equ 0 (
    echo.
    echo ✓ Conexion a la base de datos verificada
) else (
    echo.
    echo ✗ Error en la conexion
    echo Verifique que:
    echo   - PostgreSQL este ejecutandose (Services en Windows)
    echo   - Las credenciales usuario/password sean correctas
)

echo.
echo Base de datos lista. Ejecuta: mvn spring-boot:run
echo.
pause
