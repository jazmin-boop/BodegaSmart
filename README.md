# BodegaSmart - Funcionalidades de la Aplicación

**BodegaSmart** es un sistema de gestión integral y punto de venta (POS) diseñado para bodegas y pequeños comercios.

---

## 1. Venta Rápida (Punto de Venta - POS)
* **Carrito de Compras en Tiempo Real:** Selección rápida de productos con actualización automática de subtotales, totales y vuelto.
* **Escaneo de Código de Barras y QR:** Integración con la cámara del dispositivo para agregar productos al carrito mediante escaneo de código de barras o QR en tiempo real.
* **Múltiples Métodos de Cobro:** Opción para procesar ventas al contado o registrarlas directamente como crédito ("Fiado").
* **Generación de Comprobantes:** Emisión e impresión/visualización de boleta digital con el detalle de la compra.

---

## 2. Control y Gestión de Fiados (Créditos)
* **Directorio de Clientes Deudores:** Registro detallado de clientes con cuentas fiadas y saldos pendientes.
* **Historial de Movimientos:** Seguimiento de cada compra realizada a crédito por cliente.
* **Registro de Abonos y Pagos:** Actualización instantánea del saldo deudor tras registrar abonos parciales o cancelaciones totales.

---

## 3. Gestión de Inventario
* **Administración de Productos:** Registro, modificación y eliminación de productos con precio, costo, categoría y stock.
* **Alertas de Stock Bajo:** Indicadores visuales y filtros inmediatos para identificar productos con existencias mínimas.
* **Alertas de Vencimiento:** Notificaciones e indicadores automáticos para productos próximos a vencer.
* **Búsqueda y Filtros:** Organización y búsqueda rápida de productos por nombre, categoría o estado de stock/vencimiento.

---

## 4. Reportes y Estadísticas
* **Resumen de Ventas:** Visualización de ingresos totales, volumen de ventas y margen de ganancia.
* **Filtros Temporales:** Consultas de ventas por día, semana o mes.
* **Métricas Clave:** Identificación de los productos más vendidos y comportamiento de compras.

---

## 5. Panel Principal e Información en Tiempo Real
* **Información Climática:** Consulta en tiempo real de temperatura y alertas meteorológicas (vía API Open-Meteo) para anticipar demanda de ciertos productos.
* **Resumen de Alertas:** Acceso directo desde el panel a productos con stock crítico o próximos a vencer.
* **Saludo Dinámico:** Personalización según la hora del día.

---

## 6. Sincronización y Respaldo
* **Base de Datos Local:** Funcionamiento continuo mediante almacenamiento local estructurado (Room Database).
* **Respaldo en la Nube:** Sincronización y copia de seguridad periódica mediante Firebase.
