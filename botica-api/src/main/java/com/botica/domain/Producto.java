package com.botica.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Cubre cualquier cosa que la botica compre y venda (medicamentos, pañales, biberones,
// jabones, alcohol, suplementos, insumos de curación...). "categoria" decide qué campos
// mostrar en el formulario dinámico del frontend.
//
// "atributos" guarda lo específico de cada categoría como JSON (concentración de un
// medicamento, talla de un pañal, capacidad de un biberón...). Así, agregar una categoría
// nueva con sus propios campos no requiere una migración de base de datos.
//
// Todo se VENDE por unidad (unidadVenta, ej. TABLETA) a un precio único. Cuántas unidades
// trae cada caja NO es dato del producto: se captura en cada compra (compra_detalle.unidades_por_caja),
// porque puede variar entre compras. El stock del lote siempre está en unidades. Dos tamaños de paquete distintos
// (pañales x30 y x50) son DOS productos separados, cada uno con su propio precio, no
// presentaciones de un mismo producto.
//
// "Marca" y "Fabricante" no son campos de producto: eso ya lo resuelven laboratorios
// (fabricante) y proveedores (a quién se le compra).
@Entity
@Table(name = "producto")
@Getter @Setter
public class Producto implements Activable, Prototipo<Producto> {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String descripcion;
    private String categoria;          // MEDICAMENTO, SUPLEMENTO, PANALES, BIBERON, HIGIENE,
                                        // CUIDADO_PERSONAL, CURACION, DISPOSITIVO_MEDICO, ACCESORIO, OTRO
    private Boolean requiereReceta = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> atributos = new HashMap<>();

    private String unidadVenta;        // TABLETA, AMPOLLA, UNIDAD, FRASCO, PAQUETE... (todo se vende por unidad)
    private BigDecimal precioVenta;    // precio único, por unidadVenta
    private Boolean activo = true;

    @ManyToMany
    @JoinTable(name = "producto_laboratorio",
            joinColumns = @JoinColumn(name = "producto_id"),
            inverseJoinColumns = @JoinColumn(name = "laboratorio_id"))
    private Set<Laboratorio> laboratorios = new HashSet<>();

    // Prototype: copia lista para guardarse como un producto NUEVO (sin id). Sirve de plantilla,
    // p. ej. pañales x30 -> pañales x50, que según el modelo son dos productos separados.
    //  - atributos (JSON libre) se copia en profundidad: mapas y listas anidados se duplican.
    //  - laboratorios: se copia el conjunto, pero los Laboratorio son los mismos del catálogo
    //    (referencias, no duplicados): el producto copiado se asocia a los mismos fabricantes.
    @Override
    public Producto clonar() {
        Producto copia = new Producto();           // id queda null: es otra fila
        copia.nombre = nombre;
        copia.descripcion = descripcion;
        copia.categoria = categoria;
        copia.requiereReceta = requiereReceta;
        copia.atributos = atributos == null ? new HashMap<>() : copiarMapa(atributos);
        copia.unidadVenta = unidadVenta;
        copia.precioVenta = precioVenta;
        copia.activo = activo;
        copia.laboratorios = new HashSet<>(laboratorios);
        return copia;
    }

    private static Map<String, Object> copiarMapa(Map<?, ?> origen) {
        Map<String, Object> copia = new LinkedHashMap<>();
        origen.forEach((k, v) -> copia.put(String.valueOf(k), copiaProfunda(v)));
        return copia;
    }

    private static Object copiaProfunda(Object valor) {
        if (valor instanceof Map<?, ?> m) return copiarMapa(m);
        if (valor instanceof List<?> l) {
            List<Object> copia = new ArrayList<>(l.size());
            l.forEach(x -> copia.add(copiaProfunda(x)));
            return copia;
        }
        return valor; // String, Number, Boolean o null: inmutables, se comparten sin riesgo
    }
}
