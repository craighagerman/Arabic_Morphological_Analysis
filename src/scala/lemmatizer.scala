





case class DictEntry (
    entry: String,
    lemmaID: String,
    vocalization: String,
    morphology: String,
    gloss: String,
    POS: Array[String] )



def load_dict(filename: String): scala.collection.mutable.Map[String, Array[List[String]]] = {

    val dict = scala.collection.mutable.Map[String, Array[DictEntry]]()
    var lemmaID = ""


    val lines = Source.fromFile(filename).getLines.toList
    for (line <- lines) {
        if (line.startsWith(";; ")) {
            lemmaID = line.slice(3, line.length).trim
        }
        else if (line.startsWith(";")) {}
        else {
            val col = line.split("\t", -1)
            val entry = col(0)
            val vocalization = col(1)
            val morphology = col(2)
            val gloss = col(3).split("<pos")(0).trim
            val POS = getPOS(col(3), morphology, vocalization, gloss)

        val entry = "craig"
        val lemmaID = "Craig Hagerman"
        val vocaliation = "daddy"
        val morphology = "Noun"
        val POS = Array("N", "Adj")

        val anEntry = DictEntry(entry, lemmaID, vocalization, morphology, gloss, POS)
        
        val tmpArray = dict.get(entry).map(_ :+ anEntry).getOrElse[Array[DictEntry]](Array(anEntry))
        dict(entry) = tmpArray
    }
    return dict
}



