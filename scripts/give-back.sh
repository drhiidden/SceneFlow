#!/bin/bash

# give-back.sh — Auto-star script para SceneFlow
# Si el setup fue exitoso, pregunta al usuario si quiere dar una estrella al repo

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🎁 SceneFlow — Give Back${NC}"
echo ""

# 1. Verificar que SceneFlow está instalado en Maven local
MAVEN_LOCAL=~/.m2/repository/com/drhidden/sceneflow

if [ ! -d "$MAVEN_LOCAL" ]; then
    echo "❌ SceneFlow no encontrado en Maven local."
    echo "   Instala primero:"
    echo "   mvn clean install"
    exit 1
fi

echo "✅ SceneFlow instalado en Maven local"

# 2. Verificar que los tests pasaron (opcional, verificar target/surefire-reports)
if [ -d "target/surefire-reports" ]; then
    TEST_FAILURES=$(grep -r "Failures: 0" target/surefire-reports/*.txt 2>/dev/null || echo "")
    if [ -n "$TEST_FAILURES" ]; then
        echo "✅ Tests ejecutados correctamente (0 failures)"
    else
        echo "⚠️  Algunos tests fallaron. Revisa target/surefire-reports/"
    fi
fi

echo ""

# 3. Verificar que gh CLI está instalado
if ! command -v gh &> /dev/null; then
    echo "⚠️  gh CLI no encontrado. Necesitas instalarlo para dar estrella automáticamente."
    echo ""
    echo "Opciones:"
    echo "  1. Instalar gh: https://cli.github.com/"
    echo "  2. Dar estrella manualmente: https://github.com/drhiidden/SceneFlow"
    exit 0
fi

# 4. Verificar que gh está autenticado
if ! gh auth status &> /dev/null; then
    echo "⚠️  gh CLI no está autenticado."
    echo ""
    echo "Ejecuta: gh auth login"
    echo "Luego vuelve a ejecutar este script."
    exit 1
fi

# 5. Preguntar al usuario
echo "✅ SceneFlow está funcionando correctamente."
echo ""
echo "Si te gustó el framework, considera darle una estrella en GitHub."
echo "Ayuda a que más gente descubra el proyecto."
echo ""
read -p "¿Quieres dar una estrella ahora? (s/n): " -n 1 -r
echo ""

if [[ $REPLY =~ ^[SsYy]$ ]]; then
    echo ""
    echo "⭐ Dando estrella a drhiidden/SceneFlow..."
    
    if gh repo star drhiidden/SceneFlow 2>&1 | grep -q "already starred"; then
        echo -e "${YELLOW}Ya habías dado estrella antes. ¡Gracias!${NC}"
    else
        echo -e "${GREEN}✅ ¡Estrella dada! Gracias por tu apoyo.${NC}"
    fi
else
    echo ""
    echo "No hay problema. Si cambias de opinión:"
    echo "  → https://github.com/drhiidden/SceneFlow"
    echo ""
    echo "¡Gracias por usar SceneFlow!"
fi
