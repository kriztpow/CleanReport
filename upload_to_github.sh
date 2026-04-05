#!/data/data/com.termux/files/usr/bin/bash
echo "🚀 Subiendo CleanReport a GitHub"

# 1. Instalación rápida si falta Git
if ! command -v git &> /dev/null; then
  pkg update -y && pkg install git -y
fi

# 2. Configuración inteligente (solo pide si no existe)
GIT_USER=$(git config --global user.name)
if [ -z "$GIT_USER" ]; then
  read -p "👤 Usuario Git: " GIT_NAME
  git config --global user.name "$GIT_NAME"
fi

GIT_MAIL=$(git config --global user.email)
if [ -z "$GIT_MAIL" ]; then
  read -p "📧 Email Git: " GIT_EMAIL
  git config --global user.email "$GIT_EMAIL"
fi

# 3. Preparar Directorio
PROJECT_DIR=$(pwd)
git config --global --add safe.directory "$PROJECT_DIR"
[ ! -d ".git" ] && git init

# 4. Configurar Remoto (Mejorado para detectar URL)
CURRENT_REMOTE=$(git remote get-url origin 2>/dev/null)
if [ -z "$CURRENT_REMOTE" ]; then
  read -p "🌐 URL del repo (ej: https://github.com/tu-user/CleanReport.git): " GITHUB_URL
  git remote add origin "$GITHUB_URL"
else
  echo "✅ Remoto detectado: $CURRENT_REMOTE"
fi

# 5. Gitignore reforzado para Ktor y Android
if [ ! -f ".gitignore" ]; then
cat <<EOL > .gitignore
.gradle/
build/
/build/
app/build/
captures/
.externalNativeBuild
.cxx
local.properties
*.keystore
*.jks
.idea/
.vscode/
*.apk
bin/
gen/
EOL
fi

# 6. Limpieza de archivos grandes (>100MB)
find . -type f -size +100M > big_files.txt
if [ -s big_files.txt ]; then
  echo "⚠️ Archivos grandes detectados, ignorando..."
  while IFS= read -r file; do
    echo "$file" >> .gitignore
    git rm --cached "$file" 2>/dev/null
  done < big_files.txt
  rm big_files.txt
fi

# 7. Commit y Push
git add .
read -p "📝 Mensaje de commit (Enter para defecto): " COMMIT_MSG
[ -z "$COMMIT_MSG" ] && COMMIT_MSG="Update CleanReport: Shield and Server Logic"

git commit -m "$COMMIT_MSG" || echo "No hay cambios para subir"
git branch -M main

echo "📤 Subiendo a GitHub..."
git push -u origin main || git push -f origin main

echo "✅ Proceso finalizado correctamente"
