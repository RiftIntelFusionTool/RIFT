package dev.nohus.rift.repositories

object WormholeRegionClasses {
    private val classes = mapOf(
        "A-R00001" to "Class 1",
        "A-R00002" to "Class 1",
        "A-R00003" to "Class 1",
        "B-R00004" to "Class 2",
        "B-R00005" to "Class 2",
        "B-R00006" to "Class 2",
        "B-R00007" to "Class 2",
        "B-R00008" to "Class 2",
        "C-R00009" to "Class 3",
        "C-R00010" to "Class 3",
        "C-R00011" to "Class 3",
        "C-R00012" to "Class 3",
        "C-R00013" to "Class 3",
        "C-R00014" to "Class 3",
        "C-R00015" to "Class 3",
        "D-R00016" to "Class 4",
        "D-R00017" to "Class 4",
        "D-R00018" to "Class 4",
        "D-R00019" to "Class 4",
        "D-R00020" to "Class 4",
        "D-R00021" to "Class 4",
        "D-R00022" to "Class 4",
        "D-R00023" to "Class 4",
        "E-R00024" to "Class 5",
        "E-R00025" to "Class 5",
        "E-R00026" to "Class 5",
        "E-R00027" to "Class 5",
        "E-R00028" to "Class 5",
        "E-R00029" to "Class 5",
        "F-R00030" to "Class 6",
        "G-R00031" to "Class 12", // Thera
        "H-R00032" to "Class 13",
        "K-R00033" to "Drifter",
    )

    operator fun get(regionName: String) = classes[regionName]
}
