# Examen-Analisis-Algoritmico-Avanzado
Examen del segundo semestre de la clase de Análisis Algorítmico Avanzado, asignado por la profesora Evelyn (finalmente algo que me obliga a crear un repositorio)

# Cálculos de BigO y T(n)
**Lucifer**

Cálculo de T(n): T(n) = $\frac{n}{2} \times (16 \times k)$

Donde **k** es el tiempo de las operaciones constantes dentro de la red.

Simplificando constantes: **T(n) = c $\cdot$ n**

Complejidad Big O: **O(n)** (Lineal). El tiempo de ejecución crece **proporcionalmente** al tamaño del texto.


**DES**

Cálculo de T(n): T(n) = $\frac{n}{8} \times$ (Operaciones fijas por bloque DES)

Al eliminar constantes: **T(n) = c $\cdot$ n**

Complejidad Big O: **O(n)** (Lineal).


**Hash SHA-256**

Cálculo de T(n): T(n) = $\frac{n}{64} \times$ (64 rondas de compresión)

Complejidad Big O: **O(n)** (Lineal).
