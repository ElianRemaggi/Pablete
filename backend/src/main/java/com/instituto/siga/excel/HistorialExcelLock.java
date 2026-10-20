package com.instituto.siga.excel;

import java.util.concurrent.locks.ReentrantLock;

/** Un unico candado para todo acceso al .xlsx dentro del proceso: evita que una lectura (sincronizacion)
 * vea el archivo a mitad de un reemplazo, y que dos escrituras se pisen. No protege contra otros
 * procesos (p. ej. Excel abierto): ese caso se detecta como IOException y se informa. */
final class HistorialExcelLock {

    static final ReentrantLock LOCK = new ReentrantLock();

    private HistorialExcelLock() {
    }
}
