# Guía de Demostración - NOMAD MCP

Esta guía proporciona un script completo para realizar una demo efectiva de NOMAD ante clientes, usando la ruta Madrid-Toledo como caso de uso.

## Índice

1. [Preparación Pre-Demo](#preparación-pre-demo)
2. [Script de Demostración](#script-de-demostración)
3. [Casos de Uso a Demostrar](#casos-de-uso-a-demostrar)
4. [Puntos Clave de Valor](#puntos-clave-de-valor)
5. [Manejo de Preguntas Frecuentes](#manejo-de-preguntas-frecuentes)

---

## Preparación Pre-Demo

### Checklist 24 Horas Antes

- [ ] Backend desplegado y funcionando en Railway
- [ ] URL de Railway configurada en la app Android
- [ ] App instalada en dispositivo físico
- [ ] Dispositivo con batería >80%
- [ ] Datos móviles o WiFi portátil disponible
- [ ] Crédito suficiente en OpenAI API ($5 mínimo)
- [ ] Google Places API key activa
- [ ] Laptop con Railway dashboard para monitoreo

### Checklist 1 Hora Antes

- [ ] Verificar conectividad del backend:
```bash
curl https://TU-URL-RAILWAY.up.railway.app/api/actuator/health
```
- [ ] Abrir NOMAD y verificar que carga POIs
- [ ] Probar asistente de voz una vez
- [ ] Verificar que GPS funciona correctamente
- [ ] Tener abierto Railway dashboard en laptop
- [ ] Preparar notebook para tomar feedback

### Material de Apoyo

1. **Dispositivo Android** con NOMAD instalado
2. **Laptop** con:
   - Railway dashboard abierto (mostrar métricas en vivo)
   - Google Maps con ruta Madrid-Toledo
   - Documento de arquitectura técnica (opcional)
3. **Cargador portátil** por si la demo se extiende
4. **Script impreso** de esta guía

---

## Script de Demostración

### Introducción (2 minutos)

**Narrativa**:

> "Buenos días. Hoy les voy a mostrar NOMAD, un asistente de viaje inteligente que revoluciona la experiencia turística en carretera.
>
> A diferencia de los navegadores tradicionales que solo te llevan del punto A al punto B, NOMAD enriquece tu viaje descubriendo y explicando puntos de interés a lo largo de la ruta, como un guía turístico personal que te acompaña.
>
> Voy a demostrarles cómo funciona usando la ruta Madrid-Toledo, un trayecto de unos 70km que normalmente se hace sin ningún tipo de información turística. Con NOMAD, este viaje se convierte en una experiencia educativa y cultural."

**Mostrar en pantalla**:
- Abrir NOMAD en el dispositivo
- Mostrar el mapa inicial centrado en Madrid

### Demo 1: Descubrimiento Automático de POIs (5 minutos)

**Acción**:
1. Mostrar el mapa con la ubicación actual
2. Señalar los marcadores de diferentes colores en el mapa

**Narrativa**:

> "Como pueden ver, NOMAD detecta automáticamente puntos de interés cerca de nuestra ubicación.
>
> Los colores representan diferentes categorías:
> - **Amarillo**: Historia (monumentos, sitios históricos)
> - **Morado**: Gastronomía (restaurantes, mercados tradicionales)
> - **Azul**: Arte (museos, galerías)
> - **Verde**: Deportes (estadios, instalaciones)
> - **Naranja**: Geografía (parques, miradores)
> - **Gris**: Industria (fábricas históricas, edificios industriales)
>
> Puedo filtrar por categoría tocando estos chips en la parte superior."

**Demostración**:
1. Tocar el chip "Historia"
2. Mostrar cómo el mapa se actualiza mostrando solo POIs históricos
3. Tocar un marcador amarillo (ejemplo: Palacio Real, Catedral de la Almudena)

**Narrativa continuada**:

> "Al tocar un punto de interés, vemos información básica: nombre, categoría y distancia.
> Pero aquí viene la magia de NOMAD..."

### Demo 2: Asistente de Voz Contextual (7 minutos)

**Acción**:
1. En el bottom sheet del POI, tocar "Pregunta al asistente"
2. El asistente comenzará a hablar automáticamente sobre el POI

**Narrativa**:

> "El asistente de voz de NOMAD, potenciado por GPT-4, conoce el contexto completo:
> - Dónde estamos
> - Qué estamos mirando
> - Qué categorías nos interesan
>
> Escuchen la explicación..."

**Después de que termine de hablar**:

> "Esto es solo el comienzo. Puedo mantener una conversación natural con el asistente."

**Demostración de conversación**:

1. Presionar el botón de voz flotante (azul)
2. **Preguntar**: "¿Cuál es la mejor época para visitarlo?"
   - *El asistente responderá con información específica*

3. **Preguntar**: "¿Hay algún restaurante tradicional cerca?"
   - *El asistente mencionará opciones gastronómicas cercanas*

4. **Preguntar**: "Cuéntame algo curioso sobre este lugar"
   - *El asistente compartirá anécdotas o datos interesantes*

**Narrativa continuada**:

> "Como ven, el asistente mantiene el contexto de la conversación y puede responder preguntas de seguimiento de manera natural, como si estuvieras hablando con un guía turístico experto."

### Demo 3: Uso en Conducción - Modo Manos Libres (5 minutos)

**Narrativa**:

> "NOMAD está diseñado específicamente para uso mientras conduces. Todas las interacciones principales son por voz, sin necesidad de tocar la pantalla.
>
> Imaginen que están conduciendo de Madrid a Toledo. Así es como usarían NOMAD:"

**Simulación de conducción**:

1. **Activar asistente por voz**:
   - Presionar botón de voz
   - **Decir**: "¿Qué lugares interesantes hay en mi ruta?"
   - *El asistente listará POIs relevantes*

2. **Preguntar por categoría específica**:
   - Presionar botón de voz
   - **Decir**: "Muéstrame sitios históricos"
   - *El asistente responderá sobre lugares históricos*

3. **Solicitar información detallada**:
   - Presionar botón de voz
   - **Decir**: "Cuéntame sobre el Alcázar de Toledo"
   - *El asistente explicará la historia del Alcázar*

**Narrativa continuada**:

> "Noten cómo las respuestas son concisas y claras, optimizadas para escuchar mientras conduces. No son textos largos, sino explicaciones de 30-60 segundos que no distraen del camino."

### Demo 4: Personalización y Ajustes (3 minutos)

**Acción**:
1. Tocar el icono de ajustes (⚙️) arriba a la derecha
2. Mostrar las opciones de configuración

**Narrativa**:

> "NOMAD es completamente personalizable según tus preferencias:
>
> **Radio de búsqueda**: Define qué tan lejos quieres descubrir POIs (100m a 5km)
> - Útil para viajes largos vs exploración urbana
>
> **Velocidad de narración**: Ajusta qué tan rápido habla el asistente
> - Para personas que prefieren explicaciones más rápidas o lentas
>
> **Tono de voz**: Personaliza la entonación del asistente
> - Hace la experiencia más natural y agradable
>
> **URL del Backend**: Transparencia total sobre dónde se procesan los datos
> - Posibilidad de usar backend privado corporativo"

**Mostrar cambio en vivo**:
1. Cambiar velocidad de narración a 1.5x
2. Volver al mapa
3. Probar el asistente brevemente
4. Mostrar que la voz es más rápida

### Demo 5: Tecnología y Arquitectura (3 minutos)

**Mostrar en laptop**:
- Railway dashboard con métricas en vivo

**Narrativa técnica** (si la audiencia es técnica):

> "Detrás de escena, NOMAD utiliza una arquitectura moderna y eficiente:
>
> **Backend en la nube** (Railway):
> - Spring Boot con Java 21
> - Desplegado en contenedor Docker
> - Auto-escalable según demanda
> - Monitoreo en tiempo real
>
> **Fuentes de datos**:
> - Google Places API (cobertura global, datos actualizados)
> - Cache inteligente (10 minutos, reduce costos 80%)
>
> **IA conversacional**:
> - Sistema híbrido patentado
> - Android STT (gratis) + GPT-4 + Android TTS (gratis)
> - 97% más barato que soluciones tradicionales
> - Costo: ~$0.50/hora vs $18/hora de OpenAI Realtime API
>
> **Optimizaciones**:
> - Actualización inteligente de POIs (solo cada 200m de movimiento)
> - Caché local en dispositivo
> - Consumo mínimo de batería y datos"

**Mostrar métricas**:
1. Uso de CPU/RAM en Railway
2. Número de requests procesadas
3. Tiempo de respuesta promedio

---

## Casos de Uso a Demostrar

### Caso 1: Turista Extranjero

**Escenario**:
> "Imaginen un turista de Estados Unidos que alquila un coche en Madrid y quiere explorar Castilla-La Mancha."

**Demo**:
1. Mostrar cómo NOMAD detecta automáticamente POIs en inglés (si está configurado)
2. Explicar lugares que un GPS normal ignoraría
3. Recomendar restaurantes tradicionales para experiencia local

**Valor**:
- No necesita investigar antes del viaje
- Descubrimientos espontáneos
- Información contextual inmediata

### Caso 2: Familia con Niños

**Escenario**:
> "Una familia española viaja de Madrid a Toledo un fin de semana."

**Demo**:
1. Usar categoría "Historia" para educar a los niños
2. Preguntar al asistente: "¿Hay alguna historia interesante para niños sobre este castillo?"
3. Encontrar parques o áreas recreativas en la ruta

**Valor**:
- Viaje educativo y entretenido
- Los niños aprenden mientras viajan
- Descubren paradas interesantes para estirar las piernas

### Caso 3: Profesional en Viaje de Negocios

**Escenario**:
> "Un ejecutivo viaja entre ciudades para reuniones y tiene 1-2 horas libres."

**Demo**:
1. Buscar gastronomía cerca de la oficina del cliente
2. Encontrar museos o sitios culturales para visita rápida
3. Optimizar tiempo libre aprendiendo sobre la ciudad

**Valor**:
- Maximiza tiempo libre
- Experiencia cultural incluso en viajes de trabajo
- Recomendaciones locales auténticas

### Caso 4: Ruta Enológica / Gastronómica

**Escenario**:
> "Entusiastas del vino/gastronomía exploran regiones productoras."

**Demo**:
1. Filtrar por categoría "Gastronomía"
2. Descubrir bodegas, mercados locales, restaurantes tradicionales
3. Preguntar: "¿Qué plato típico debería probar aquí?"

**Valor**:
- Experiencias culinarias auténticas
- Descubrir gemas escondidas
- Información de primera mano sobre tradiciones locales

---

## Puntos Clave de Valor

### Para el Usuario Final

1. **Descubrimiento Pasivo**
   - No necesitas planificar: NOMAD descubre por ti
   - Serendipia: encuentros inesperados enriquecen el viaje
   - Sin esfuerzo: el asistente trabaja mientras tú conduces

2. **Experiencia Educativa**
   - Aprende historia, cultura, gastronomía
   - Contexto profundo, no solo nombres
   - Conversación natural, como hablar con un amigo conocedor

3. **Seguridad en Conducción**
   - Manos libres total
   - Respuestas concisas (no distraen)
   - Diseño optimizado para visibilidad al sol

4. **Ahorro de Tiempo**
   - No necesitas buscar en Google cada lugar
   - No necesitas leer guías turísticas
   - Información justo cuando la necesitas

### Para el Negocio (B2B)

1. **Coste-Eficiencia**
   - Sistema híbrido: 97% más barato que alternativas
   - Escalable: de 10 a 10,000 usuarios sin cambios arquitectónicos
   - Cloud-native: sin infraestructura propia

2. **Datos y Analytics**
   - Métricas de uso en tiempo real
   - Preferencias de usuarios (categorías favoritas)
   - Optimización basada en comportamiento real

3. **Personalización Corporativa**
   - Backend privado para datos sensibles
   - Branding customizable
   - Integraciones con sistemas existentes

4. **Modelo de Negocio**
   - Freemium: Gratis con límites, premium ilimitado
   - B2B: Licencias para empresas turísticas, rent-a-car
   - Affiliate: Comisiones por reservas de restaurantes/hoteles

---

## Manejo de Preguntas Frecuentes

### "¿Funciona sin conexión a internet?"

**Respuesta**:
> "Actualmente NOMAD requiere conexión a internet para dos funciones críticas:
> 1. Obtener POIs actualizados de Google Places
> 2. Generar respuestas inteligentes con GPT-4
>
> Sin embargo, estamos desarrollando modo offline que:
> - Pre-carga POIs de la ruta planificada
> - Cachea información básica
> - Funcionalidad reducida pero útil sin conexión
>
> Para la mayoría de usuarios, los datos móviles son suficientes. El consumo es bajo: ~5-10 MB/hora."

### "¿Qué pasa con mi privacidad y datos de ubicación?"

**Respuesta**:
> "La privacidad es una prioridad:
>
> **Datos de ubicación**:
> - Se usan solo en tiempo real para buscar POIs cercanos
> - NO se almacenan permanentemente
> - NO se comparten con terceros (excepto Google Places para la búsqueda)
>
> **Conversaciones de voz**:
> - Se envían a OpenAI para procesamiento (encriptadas)
> - NO se guardan en nuestros servidores
> - Opción de backend privado para empresas con requisitos estrictos
>
> **Transparencia total**:
> - Código open-source (puede auditarse)
> - Ajustes muestran exactamente qué backend se usa
> - GDPR compliant (en roadmap para lanzamiento europeo)"

### "¿Cómo se compara con Google Maps / Waze / TomTom?"

**Respuesta**:
> "NOMAD no es un reemplazo de navegadores GPS, sino un complemento:
>
> **Google Maps/Waze**:
> - Enfoque: Llegar del punto A al B lo más rápido posible
> - NOMAD: Enriquecer el viaje mientras vas de A a B
>
> **Diferencias clave**:
> 1. **Contexto profundo**: Google Maps muestra nombres, NOMAD explica historias
> 2. **Conversación natural**: Puedes hacer preguntas de seguimiento
> 3. **Descubrimiento proactivo**: NOMAD sugiere sin que preguntes
> 4. **Enfoque educativo**: Aprender es parte del viaje
>
> **Uso ideal**: Google Maps para navegar + NOMAD para descubrir"

### "¿Qué idiomas soporta?"

**Respuesta**:
> "Actualmente:
> - **Español**: 100% soportado (producción)
> - **Inglés**: En desarrollo (próxima versión)
>
> Roadmap:
> - Francés, Alemán, Italiano (Q2 2026)
> - Detección automática de idioma del dispositivo
> - Multilingüe en la misma conversación (útil para turistas)
>
> La arquitectura está diseñada para fácil internacionalización."

### "¿Cuánto cuesta?"

**Respuesta** (ajustar según modelo de negocio final):

**Para usuarios finales** (B2C):
> - **Versión Free**: 10 consultas al asistente/día + POIs ilimitados
> - **Premium**: €4.99/mes - Ilimitado
> - **Trial**: 7 días gratis de Premium
>
> **Para empresas** (B2B):
> - **Small**: €99/mes - Hasta 50 usuarios
> - **Medium**: €299/mes - Hasta 200 usuarios
> - **Enterprise**: Precio personalizado - Backend privado, soporte 24/7, SLA
>
> **Comparativa**:
> - Guía turístico humano: €150-300/día
> - NOMAD Premium: €4.99/mes (ilimitado)

### "¿Qué pasa si el asistente da información incorrecta?"

**Respuesta**:
> "Gran pregunta. Tenemos varios niveles de verificación:
>
> **1. Fuente de POIs**: Google Places (actualizado constantemente)
> **2. GPT-4**: Entrenado con datos hasta enero 2025, alta precisión
> **3. Sistema de feedback**: Los usuarios pueden reportar errores
> **4. Disclaimers**: El asistente indica cuando no está 100% seguro
>
> **Si hay error**:
> - Botón 'Reportar información incorrecta'
> - Equipo de QA revisa y actualiza
> - Sistema aprende con el tiempo
>
> **Meta**: >95% de precisión (actualmente ~92% según testing interno)"

### "¿Funciona en otros países además de España?"

**Respuesta**:
> "Sí, la tecnología es global:
>
> **Actualmente optimizado para**:
> - España (datos más completos, mejor contexto)
>
> **Funciona en** (con menor profundidad):
> - Toda Europa
> - Estados Unidos
> - América Latina
> - Cualquier lugar con cobertura de Google Places
>
> **Roadmap de expansión**:
> - Q1 2026: Francia, Italia
> - Q2 2026: Alemania, Portugal
> - Q3 2026: Reino Unido, Irlanda
> - Q4 2026: Estados Unidos
>
> El sistema es plug-and-play para nuevos países."

---

## Cierre de la Demo (2 minutos)

**Narrativa final**:

> "Hemos visto cómo NOMAD transforma un simple trayecto en coche en una experiencia educativa y cultural enriquecedora.
>
> **Recap de beneficios**:
> 1. Descubrimiento automático de lugares interesantes
> 2. Asistente de IA que responde cualquier pregunta con contexto
> 3. Totalmente manos libres y seguro para conducir
> 4. Personalizable según tus intereses
> 5. Tecnología eficiente y escalable
>
> **Próximos pasos**:
> - ¿Les gustaría probar NOMAD ustedes mismos?
> - ¿Tienen una ruta específica en mente que quieran explorar?
> - ¿Hay algún caso de uso particular para su negocio?
>
> Estamos abiertos a colaboraciones, pruebas piloto y feedback. Gracias por su tiempo."

**Call to Action**:
- Entregar tarjetas de contacto
- Ofrecer instalar la app en sus dispositivos
- Agendar follow-up meeting
- Compartir documentación técnica si están interesados

---

## Métricas de Éxito de la Demo

Después de cada demo, evaluar:

- [ ] ¿Entendieron el valor diferencial vs Google Maps?
- [ ] ¿Probaron el asistente de voz ellos mismos?
- [ ] ¿Hicieron preguntas técnicas detalladas? (señal de interés)
- [ ] ¿Mencionaron casos de uso específicos de su negocio?
- [ ] ¿Agendaron follow-up o solicitaron propuesta?

**Feedback a recopilar**:
1. ¿Qué característica les pareció más valiosa?
2. ¿Qué les falta o qué mejorarían?
3. ¿Cuánto estarían dispuestos a pagar? (si B2C)
4. ¿Cuántos usuarios tendrían? (si B2B)
5. ¿Cuál sería su caso de uso principal?

---

## Tips para una Demo Exitosa

1. **Practica antes**: Haz la demo 3-5 veces en privado
2. **Conoce tu audiencia**: Ajusta el lenguaje técnico según el perfil
3. **Cuenta historias**: Usa narrativas, no solo features
4. **Maneja errores con gracia**: Si algo falla, explica el por qué (transparencia genera confianza)
5. **Escucha más que hablas**: Las mejores demos son conversaciones
6. **Anota feedback en vivo**: Muestra que valoras su opinión
7. **Sé honesto sobre limitaciones**: Genera más confianza que exagerar
8. **Termina con siguiente paso claro**: No dejes la demo sin acción concreta

¡Buena suerte con la demo! 🚀
