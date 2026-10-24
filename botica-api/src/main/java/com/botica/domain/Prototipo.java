package com.botica.domain;

// Prototype: un objeto que sabe producir una copia independiente de sí mismo. Se usa una
// interfaz propia en vez de Cloneable/Object.clone(), que hace copias superficiales y es
// propenso a errores. La copia debe ser PROFUNDA para lo mutable que el objeto posee
// (mapas, listas): si no, original y copia compartirían estado y editar uno cambiaría el otro.
public interface Prototipo<T> {
    T clonar();
}
