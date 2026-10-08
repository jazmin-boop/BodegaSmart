# BodegaSmart 🛒

## 1. Descripción de la Aplicación
**BodegaSmart** es una aplicación móvil nativa para Android diseñada para la gestión integral y punto de venta (POS) de bodegas, minimarkets y pequeños comercios. Facilita el control diario de ventas, administración de inventarios con alertas inteligentes, seguimiento de cuentas por cobrar ("fiados"), escaneo de códigos de barras/QR y generación de reportes financieros.

---

## 2. Herramientas y Tecnologías Utilizadas

* **Lenguaje de Programación:**
  * **Java 11:** Lenguaje principal para la lógica de negocio, controladores y comunicación con la base de datos.

* **Desarrollo Android y UI:**
  * **Android SDK:** Configurado para Android 8.0+ (Min SDK 26, Target SDK 37).
  * **XML Layouts & Material Design 3:** Componentes visuales responsivos (`ConstraintLayout`, `RecyclerView`, `CardView`, diálogos personalizados).
  * **Glide:** Biblioteca para la carga y optimización eficiente de imágenes.

* **Base de Datos y Almacenamiento:**
  * **Room Persistence Library:** Base de datos relacional local sobre SQLite para el almacenamiento estructurado de productos, clientes y ventas.

* **Backend y Respaldo en la Nube:**
  * **Firebase Realtime Database:** Sincronización y copia de seguridad periódica de datos en la nube.
  * **Firebase Analytics:** Monitoreo y métricas de uso de la aplicación.

* **Lector de Códigos:**
  * **ZXing Embedded (`zxing-android-embedded`):** Escaneo mediante la cámara del dispositivo para códigos de barras y códigos QR en orientación vertical.

* **Servicios Web / APIs:**
  * **Open-Meteo REST API:** Consulta en tiempo real de temperatura y estado climático local mediante peticiones HTTP (`HttpURLConnection`) e interpretación de datos JSON.

* **Herramientas de Construcción y Control de Versiones:**
  * **Gradle (Kotlin DSL):** Gestión de dependencias y automatización del build.
  * **Git & GitHub:** Control de versiones y repositorio remoto.

---

## 3. Funcionalidades de la Aplicación

* **Punto de Venta (Venta Rápida POS):**
  * Carrito de compras con cálculo automático de totales y vuelto.
  * Agregado rápido de productos mediante escaneo de código de barras/QR.
  * Procesamiento de cobro en efectivo o registro como venta a crédito ("Fiado").
  * Emisión e impresión/visualización de boletas digitales.

* **Control de Fiados (Créditos y Deudas):**
  * Directorio de clientes con saldos pendientes.
  * Historial detallado de compras a crédito y registro de abonos o cancelación de deudas.

* **Gestión de Inventario y Alertas:**
  * Registro, edición y eliminación de productos organizados por categoría.
  * Alertas automáticas para productos con **Stock Bajo**.
  * Notificaciones e indicadores para productos **Próximos a Vencer**.

* **Reportes y Estadísticas:**
  * Resumen de ingresos, ventas realizadas y ganancias por periodos (día, semana, mes).
  * Análisis de productos más vendidos.

* **Panel Principal e Información Relevante:**
  * Saludo dinámico según la hora del día.
  * Información del clima en tiempo real para prever la demanda de productos según la temperatura.
  * Acceso directo a productos con alertas activas.

* **Respaldo y Seguridad:**
  * Operación fluida offline mediante la base de datos local Room.
  * Sincronización automática de respaldos con Firebase.
