

import scala.collection.mutable.ArrayBuffer
import scala.collection._
import scala.io.Source



////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//          Dictionary Object
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
object Dicts {
    var prefix_dict: scala.collection.mutable.Map[String, Array[List[String]]] = _
    var stem_dict: scala.collection.mutable.Map[String, Array[List[String]]] = _
    var suffix_dict: scala.collection.mutable.Map[String, Array[List[String]]] = _
    var tableAB: Set[String] = _
    var tableAC: Set[String] = _
    var tableBC: Set[String] = _


    // Load dict file into hash table
    def deducePOS(morphology: String, vocalization: String, gloss: String): String = {
        var POS = ""
        if ("^(Pref-0|Suff-0)$".r.findAllIn(morphology).length > 0)  POS = ""
        else if ("^F".r.findAllIn(morphology).length > 0)  POS = vocalization + "/FUNC_WORD"
        else if ("^IV".r.findAllIn(morphology).length > 0) POS = vocalization + "/VERB_IMPERFECT"
        else if ("^PV".r.findAllIn(morphology).length > 0) POS = vocalization + "/VERB_PERFECT"
        else if ("^CV".r.findAllIn(morphology).length > 0) POS = vocalization + "/VERB_IMPERATIVE"
        // educated guess
        else if ("^N".r.findAllIn(morphology).length > 0) {
            if ("^[A-Z]".r.findAllIn(gloss).length > 0) POS = vocalization + "/NOUN_PROP"
            else if ("iy~$".r.findAllIn(vocalization).length > 0) POS = vocalization + "/NOUN"
            else POS = vocalization + "/NOUN"
        }
        return POS
    }

    def getPOS(text: String, morphology: String, vocalization: String, gloss: String): String = {
        var POS = ""
        if ("<pos>(.+?)</pos>".r.findAllIn(text).length > 0 ) {
            POS = text.split("<pos")(1).replaceAll("</pos>", "").trim
        }
        else {
            POS = deducePOS(morphology, vocalization, gloss)
        }
        return POS
    }

    def load_dict(filename: String): scala.collection.mutable.Map[String, Array[List[String]]] = {
        // TO DO: change the data structure from Map[String, Array[List[String]]] and use something
        // like this instead:   Map[String, DictionaryObject]
        // Let the DictionaryObject handle setting and getting its fields
        // Have to also modify functions that use the returned dict
        val dict = scala.collection.mutable.Map[String, Array[List[String]]]()
        var lemmaID = ""
        // var lemmas: Set[String] = Set()
        val lines = Source.fromFile(filename).getLines.toList
        for (line <- lines) {
            if (line.startsWith(";; ")) {
                lemmaID = line.slice(3, line.length).trim
                // lemmas = lemmas + lemmaID                // not necessary?
            }
            else if (line.startsWith(";")) {}
            else {
                val col = line.split("\t", -1)
                val entry = col(0)
                val vocalization = col(1)
                val morphology = col(2)
                val gloss = col(3).split("<pos")(0).trim
                val POS = getPOS(col(3), morphology, vocalization, gloss)
            val entryList = List(entry, lemmaID, vocalization, morphology, gloss, POS)
            // val tmpArray = dict.get(entry).map(_ :+ entryList).getOrElse(entryList).toArray
            // dict(entry) = tmpArray
            if (dict.contains(entry)) {
                var tmpArray = dict(entry) :+ entryList
                dict(entry) = tmpArray
            }
            else {
                var tmpArray : Array[List[String]] = Array(entryList)
                dict(entry) = tmpArray}
            }
        }
        return dict
    }

    def loadCompatibilityTable(filename: String): Set[String] = {
        return Source.fromFile(filename).getLines.toList.filter(!_.startsWith(";")).map(_.replaceAll("\t", " ")).toSet
    }

    def get_dicts(dpr_file: String, dst_file: String, dsu_file: String): (scala.collection.mutable.Map[String, Array[List[String]]], scala.collection.mutable.Map[String, Array[List[String]]], scala.collection.mutable.Map[String, Array[List[String]]]) = {
        
        // load 3 lexicons
        if (dicts_exist()) { return (prefix_dict, stem_dict, suffix_dict) }
        println("Initializing prefix, stem & suffix dictionaries ...")
        prefix_dict = load_dict(dpr_file)
        stem_dict   = load_dict(dst_file)
        suffix_dict = load_dict(dsu_file)
        return (prefix_dict, stem_dict, suffix_dict)
    }

    def get_tables(tab_file: String, tac_file: String, tbc_file: String): (Set[String], Set[String], Set[String]) = {
        if (tables_exist()) { return (tableAB, tableAC, tableBC) }
        //load 3 compatibility tables
        // if (tableAB == null)    tableAB = loadCompatibilityTable(tab_file)
        // if (tableAC == null)    tableAC = loadCompatibilityTable(tac_file)
        // if (tableBC == null)    tableBC = loadCompatibilityTable(tbc_file)
        println("Initializing Ab, AC, BC tables ...")
        tableAB = loadCompatibilityTable(tab_file)
        tableAC = loadCompatibilityTable(tac_file)
        tableBC = loadCompatibilityTable(tbc_file)
        return (tableAB, tableAC, tableBC)
    }

    def dicts_exist() = !((prefix_dict == null) |  (stem_dict == null) | (suffix_dict == null) )
    def tables_exist() = !((tableAB == null) | (tableAC == null) | (tableBC == null))
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//           Tokenization
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

def tokenize(text: String): Array[String] = {
    val spc =  """\s+""".r
    val line = spc.replaceAllIn(text.trim(), """\s+""")
    // val tatweel = """\u0640""".r         //\u0640 : ARABIC TATWEEL (elongation)
    // val diacritics = """[FNKaui~o]""".r
    val non_arabic = """([^\u067E\u0686\u0698\u06AF\u0621-\u0636\u0637-\u0643\u0644\u0645-\u0648\u0649-\u064A\u064B-\u064E\u064F\u0650\u0651\u0652]+)""".r
    val splt = non_arabic.split(line).filter(_.length > 0)
    // val splt = line.split(" ")
    return splt
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//           Buckwalter Transliteration: Arabic -> Romanized
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def transliterate(word: String) : String = {
    var tmp = word;
    tmp = tmp.replaceAll("\u0621", "'"); //\u0621 : ARABIC LETTER HAMZA
    tmp = tmp.replaceAll("\u0622", "|"); //\u0622 : ARABIC LETTER ALEF WITH MADDA ABOVE
    tmp = tmp.replaceAll("\u0623", ">"); //\u0623 : ARABIC LETTER ALEF WITH HAMZA ABOVE
    tmp = tmp.replaceAll("\u0624", "&"); //\u0624 : ARABIC LETTER WAW WITH HAMZA ABOVE
    tmp = tmp.replaceAll("\u0625", "<"); //\u0625 : ARABIC LETTER ALEF WITH HAMZA BELOW
    tmp = tmp.replaceAll("\u0626", "}"); //\u0626 : ARABIC LETTER YEH WITH HAMZA ABOVE
    tmp = tmp.replaceAll("\u0627", "A"); //\u0627 : ARABIC LETTER ALEF
    tmp = tmp.replaceAll("\u0628", "b"); //\u0628 : ARABIC LETTER BEH
    tmp = tmp.replaceAll("\u0629", "p"); //\u0629 : ARABIC LETTER TEH MARBUTA
    tmp = tmp.replaceAll("\u062A", "t"); //\u062A : ARABIC LETTER TEH
    tmp = tmp.replaceAll("\u062B", "v"); //\u062B : ARABIC LETTER THEH
    tmp = tmp.replaceAll("\u062C", "j"); //\u062C : ARABIC LETTER JEEM
    tmp = tmp.replaceAll("\u062D", "H"); //\u062D : ARABIC LETTER HAH
    tmp = tmp.replaceAll("\u062E", "x"); //\u062E : ARABIC LETTER KHAH
    tmp = tmp.replaceAll("\u062F", "d"); //\u062F : ARABIC LETTER DAL
    tmp = tmp.replaceAll("\u0630", "*"); //\u0630 : ARABIC LETTER THAL
    tmp = tmp.replaceAll("\u0631", "r"); //\u0631 : ARABIC LETTER REH
    tmp = tmp.replaceAll("\u0632", "z"); //\u0632 : ARABIC LETTER ZAIN
    tmp = tmp.replaceAll("\u0633", "s"); //\u0633 : ARABIC LETTER SEEN
    tmp = tmp.replaceAll("\u0634", "\\$"); //\u0634 : ARABIC LETTER SHEEN
    tmp = tmp.replaceAll("\u0635", "S"); //\u0635 : ARABIC LETTER SAD
    tmp = tmp.replaceAll("\u0636", "D"); //\u0636 : ARABIC LETTER DAD
    tmp = tmp.replaceAll("\u0637", "T"); //\u0637 : ARABIC LETTER TAH
    tmp = tmp.replaceAll("\u0638", "Z"); //\u0638 : ARABIC LETTER ZAH
    tmp = tmp.replaceAll("\u0639", "E"); //\u0639 : ARABIC LETTER AIN
    tmp = tmp.replaceAll("\u063A", "g"); //\u063A : ARABIC LETTER GHAIN
    tmp = tmp.replaceAll("\u0640", "_"); //\u0640 : ARABIC TATWEEL
    tmp = tmp.replaceAll("\u0641", "f"); //\u0641 : ARABIC LETTER FEH
    tmp = tmp.replaceAll("\u0642", "q"); //\u0642 : ARABIC LETTER QAF
    tmp = tmp.replaceAll("\u0643", "k"); //\u0643 : ARABIC LETTER KAF
    tmp = tmp.replaceAll("\u0644", "l"); //\u0644 : ARABIC LETTER LAM
    tmp = tmp.replaceAll("\u0645", "m"); //\u0645 : ARABIC LETTER MEEM
    tmp = tmp.replaceAll("\u0646", "n"); //\u0646 : ARABIC LETTER NOON
    tmp = tmp.replaceAll("\u0647", "h"); //\u0647 : ARABIC LETTER HEH
    tmp = tmp.replaceAll("\u0648", "w"); //\u0648 : ARABIC LETTER WAW
    tmp = tmp.replaceAll("\u0649", "Y"); //\u0649 : ARABIC LETTER ALEF MAKSURA
    tmp = tmp.replaceAll("\u064A", "y"); //\u064A : ARABIC LETTER YEH
    tmp = tmp.replaceAll("\u064B", "F"); //\u064B : ARABIC FATHATAN
    tmp = tmp.replaceAll("\u064C", "N"); //\u064C : ARABIC DAMMATAN
    tmp = tmp.replaceAll("\u064D", "K"); //\u064D : ARABIC KASRATAN
    tmp = tmp.replaceAll("\u064E", "a"); //\u064E : ARABIC FATHA
    tmp = tmp.replaceAll("\u064F", "u"); //\u064F : ARABIC DAMMA
    tmp = tmp.replaceAll("\u0650", "i"); //\u0650 : ARABIC KASRA
    tmp = tmp.replaceAll("\u0651", "~"); //\u0651 : ARABIC SHADDA
    tmp = tmp.replaceAll("\u0652", "o"); //\u0652 : ARABIC SUKUN
    tmp = tmp.replaceAll("\u0670", "`"); //\u0670 : ARABIC LETTER SUPERSCRIPT ALEF
    tmp = tmp.replaceAll("\u0671", "{"); //\u0671 : ARABIC LETTER ALEF WASLA
    tmp = tmp.replaceAll("\u067E", "P"); //\u067E : ARABIC LETTER PEH
    tmp = tmp.replaceAll("\u0686", "J"); //\u0686 : ARABIC LETTER TCHEH
    tmp = tmp.replaceAll("\u06A4", "V"); //\u06A4 : ARABIC LETTER VEH
    tmp = tmp.replaceAll("\u06AF", "G"); //\u06AF : ARABIC LETTER GAF
    tmp = tmp.replaceAll("\u0698", "R"); //\u0698 : ARABIC LETTER JEH (no more in Buckwalter system)
    tmp = tmp.replaceAll("\u060C", ","); //\u060C : ARABIC COMMA
    tmp = tmp.replaceAll("\u061B", ";"); //\u061B : ARABIC SEMICOLON
    tmp = tmp.replaceAll("\u061F", "?"); //\u061F : ARABIC QUESTION MARK
    //Not significant for morphological analysis
    tmp = tmp.replaceAll("\u0640", ""); //\u0640 : ARABIC TATWEEL
    //Not suitable for morphological analysis : remove all vowels/diacritics, i.e. undo the job !
    tmp = tmp.replaceAll("[FNKaui~o]", "");
    //TODO : how to handle ARABIC LETTER SUPERSCRIPT ALEF and ARABIC LETTER ALEF WASLA ?
    tmp = tmp.replaceAll("[`\\{]", ""); //strip them for now
    return tmp;
    }



def arabizeWord(translitered: String) : String = {
    var tmp_word = translitered;
    // convert to transliteration
    tmp_word = tmp_word.replaceAll("'", "\u0621"); //\u0621 : ARABIC LETTER HAMZA
    tmp_word = tmp_word.replaceAll("\\|", "\u0622"); //\u0622 : ARABIC LETTER ALEF WITH MADDA ABOVE
    tmp_word = tmp_word.replaceAll(">", "\u0623"); //\u0623 : ARABIC LETTER ALEF WITH HAMZA ABOVE
    tmp_word = tmp_word.replaceAll("&", "\u0624"); //\u0624 : ARABIC LETTER WAW WITH HAMZA ABOVE
    tmp_word = tmp_word.replaceAll("<", "\u0625"); //\u0625 : ARABIC LETTER ALEF WITH HAMZA BELOW
    tmp_word = tmp_word.replaceAll("}", "\u0626"); //\u0626 : ARABIC LETTER YEH WITH HAMZA ABOVE
    tmp_word = tmp_word.replaceAll("A", "\u0627"); //\u0627 : ARABIC LETTER ALEF
    tmp_word = tmp_word.replaceAll("b", "\u0628"); //\u0628 : ARABIC LETTER BEH
    tmp_word = tmp_word.replaceAll("p", "\u0629"); //\u0629 : ARABIC LETTER TEH MARBUTA
    tmp_word = tmp_word.replaceAll("t", "\u062A"); //\u062A : ARABIC LETTER TEH
    tmp_word = tmp_word.replaceAll("v", "\u062B"); //\u062B : ARABIC LETTER THEH
    tmp_word = tmp_word.replaceAll("j", "\u062C"); //\u062C : ARABIC LETTER JEEM
    tmp_word = tmp_word.replaceAll("H", "\u062D"); //\u062D : ARABIC LETTER HAH
    tmp_word = tmp_word.replaceAll("x", "\u062E"); //\u062E : ARABIC LETTER KHAH
    tmp_word = tmp_word.replaceAll("d", "\u062F"); //\u062F : ARABIC LETTER DAL
    tmp_word = tmp_word.replaceAll("\\*", "\u0630"); //\u0630 : ARABIC LETTER THAL
    tmp_word = tmp_word.replaceAll("r", "\u0631"); //\u0631 : ARABIC LETTER REH
    tmp_word = tmp_word.replaceAll("z", "\u0632"); //\u0632 : ARABIC LETTER ZAIN
    tmp_word = tmp_word.replaceAll("s", "\u0633" ); //\u0633 : ARABIC LETTER SEEN
    tmp_word = tmp_word.replaceAll("\\$", "\u0634"); //\u0634 : ARABIC LETTER SHEEN
    tmp_word = tmp_word.replaceAll("S", "\u0635"); //\u0635 : ARABIC LETTER SAD
    tmp_word = tmp_word.replaceAll("D", "\u0636"); //\u0636 : ARABIC LETTER DAD
    tmp_word = tmp_word.replaceAll("T", "\u0637"); //\u0637 : ARABIC LETTER TAH
    tmp_word = tmp_word.replaceAll("Z", "\u0638"); //\u0638 : ARABIC LETTER ZAH
    tmp_word = tmp_word.replaceAll("E", "\u0639"); //\u0639 : ARABIC LETTER AIN
    tmp_word = tmp_word.replaceAll("g", "\u063A"); //\u063A : ARABIC LETTER GHAIN
    tmp_word = tmp_word.replaceAll("_", "\u0640"); //\u0640 : ARABIC TATWEEL
    tmp_word = tmp_word.replaceAll("f", "\u0641"); //\u0641 : ARABIC LETTER FEH
    tmp_word = tmp_word.replaceAll("q", "\u0642"); //\u0642 : ARABIC LETTER QAF
    tmp_word = tmp_word.replaceAll("k", "\u0643"); //\u0643 : ARABIC LETTER KAF
    tmp_word = tmp_word.replaceAll("l", "\u0644"); //\u0644 : ARABIC LETTER LAM
    tmp_word = tmp_word.replaceAll("m", "\u0645"); //\u0645 : ARABIC LETTER MEEM
    tmp_word = tmp_word.replaceAll("n", "\u0646"); //\u0646 : ARABIC LETTER NOON
    tmp_word = tmp_word.replaceAll("h", "\u0647"); //\u0647 : ARABIC LETTER HEH
    tmp_word = tmp_word.replaceAll("w", "\u0648"); //\u0648 : ARABIC LETTER WAW
    tmp_word = tmp_word.replaceAll("Y", "\u0649"); //\u0649 : ARABIC LETTER ALEF MAKSURA
    tmp_word = tmp_word.replaceAll("y", "\u064A"); //\u064A : ARABIC LETTER YEH
    tmp_word = tmp_word.replaceAll("F", "\u064B"); //\u064B : ARABIC FATHATAN
    tmp_word = tmp_word.replaceAll("N", "\u064C"); //\u064C : ARABIC DAMMATAN
    tmp_word = tmp_word.replaceAll("K", "\u064D"); //\u064D : ARABIC KASRATAN
    tmp_word = tmp_word.replaceAll("a", "\u064E"); //\u064E : ARABIC FATHA
    tmp_word = tmp_word.replaceAll("u", "\u064F"); //\u064F : ARABIC DAMMA
    tmp_word = tmp_word.replaceAll("i", "\u0650"); //\u0650 : ARABIC KASRA
    tmp_word = tmp_word.replaceAll("~", "\u0651"); //\u0651 : ARABIC SHADDA
    tmp_word = tmp_word.replaceAll("o", "\u0652"); //\u0652 : ARABIC SUKUN
    tmp_word = tmp_word.replaceAll("`", "\u0670"); //\u0670 : ARABIC LETTER SUPERSCRIPT ALEF
    tmp_word = tmp_word.replaceAll("\\{", "\u0671"); //\u0671 : ARABIC LETTER ALEF WASLA
    tmp_word = tmp_word.replaceAll("P", "\u067E"); //\u067E : ARABIC LETTER PEH
    tmp_word = tmp_word.replaceAll("J", "\u0686"); //\u0686 : ARABIC LETTER TCHEH
    tmp_word = tmp_word.replaceAll("V", "\u06A4"); //\u06A4 : ARABIC LETTER VEH
    tmp_word = tmp_word.replaceAll("G", "\u06AF"); //\u06AF : ARABIC LETTER GAF
    tmp_word = tmp_word.replaceAll("R", "\u0698"); //\u0698 : ARABIC LETTER JEH (no more in Buckwalter system)
    //Not in Buckwalter system \u0679 : ARABIC LETTER TTEH
    //Not in Buckwalter system \u0688 : ARABIC LETTER DDAL
    //Not in Buckwalter system \u06A9 : ARABIC LETTER KEHEH
    //Not in Buckwalter system \u0691 : ARABIC LETTER RREH
    //Not in Buckwalter system \u06BA : ARABIC LETTER NOON GHUNNA
    //Not in Buckwalter system \u06BE : ARABIC LETTER HEH DOACHASHMEE
    //Not in Buckwalter system \u06C1 : ARABIC LETTER HEH GOAL
    //Not in Buckwalter system \u06D2 : ARABIC LETTER YEH BARREE
    tmp_word = tmp_word.replaceAll(",", "\u060C" ); //\u060C : ARABIC COMMA
    tmp_word = tmp_word.replaceAll(";", "\u061B"); //\u061B : ARABIC SEMICOLON
    tmp_word = tmp_word.replaceAll("\\?", "\u061F"); //\u061F : ARABIC QUESTION MARK
    return tmp_word
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//           Segment word into all all possible
//           (prefix, stem, suffix) possibilities
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
def segment(word: String): Array[List[String]] = {
    val segments: ArrayBuffer[List[String]] = ArrayBuffer()
    for (i <- 0 until (5 min word.length) ) {
        var prefix = word.slice(0, i)
        // println("prefix: " + prefix)
        var remain = word.slice(i, word.length)
        // println("remain: " + remain)
        for (j <- remain.length until (0 max remain.length-7) by -1 ) {   // decrement
            val suffix = remain.slice(j,remain.length)
            val stem = remain.slice(0, j)
            // print("\tstem:   " + stem)
            // println("\tsuffix: " + suffix)
            // println(prefix + "\t" + stem + "\t" + suffix)
            segments += List(prefix, stem, suffix)
        }
    }
    return segments.toArray
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//           Find solutions, get lemmas for word
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// check if the morphology of pref and stem match. Return boolean
def isCompatible(x: List[String], y: List[String], table: Set[String]): Boolean = {
    val pair = x(3) + " " + y(3)
    val res = table.contains(pair.trim)
    return res
}


def wordSolutions(token: String,
                    prefix_dict: scala.collection.mutable.Map[String, Array[List[String]]],
                    stem_dict: scala.collection.mutable.Map[String, Array[List[String]]],
                    suffix_dict: scala.collection.mutable.Map[String, Array[List[String]]],
                    tableAB: Set[String],
                    tableBC: Set[String],
                    tableAC: Set[String]): Set[List[String]]  = {
    var solutions : Array[List[String]] = Array()
    val segments: Array[List[String]] = segment(token)
    for (seg <- segments) {
        val (pref, stem, suff) = seg match {case List(a, b, c) => (a, b, c)}
        // check if prefix, stem and suffix are known
        if (prefix_dict.contains(pref) & stem_dict.contains(stem) & suffix_dict.contains(suff)) {
            // check compatibiliity
            val prefix_alts = prefix_dict(pref)
            for (pa <- prefix_alts) {
                val stem_alts = stem_dict(stem)
                for (sta <- stem_alts) {
                    val lemmaID = sta(1)
                    if (isCompatible(pa, sta, tableAB) ) {
                        // println("AB compatible: \t"+ pa + "\t" + sta)
                        val suffix_alts = suffix_dict(suff)
                        for (sfa <- suffix_alts) {
                            if (isCompatible(pa, sfa, tableAC)) {
                                // println("AC")
                            }
                            if (isCompatible(sta, sfa, tableBC)) {
                                // println("BC")
                            }
                            if (isCompatible(pa, sfa, tableAC) & isCompatible(sta, sfa, tableBC)) {
                                // println("*** pre: " + pref + "  stem: " + stem  + "  suff: " + suff  + "\t pre:" + pa.toString + "  ste: " + sta + "  suf: " + sfa)
                                val solList = List(pref, stem, suff, lemmaID)
                                solutions = solutions :+ solList
                            }
                        }
                    }
                }
            }
        }
    }
    return solutions.toSet
}


def solutionLemmas(solutions: Set[List[String]]): List[String] =  return solutions.toList.map(_(3))


def inLexiconLemma(solLemmas: List[String], posSet: Set[String], negSet: Set[String]): String = {
    val p = solLemmas.toSet.intersect(posSet).toList
    val n = solLemmas.toSet.intersect(negSet).toList
    if (p.length > n.length) return p(0)
    else if (p.length < n.length) return n(0)
    return ""
}



def init() = {
    val dpr_file = "/Users/chagerman/Code/Arabic_Morphological_Analysis/data/dictprefixes_utf8"
    val dst_file = "/Users/chagerman/Code/Arabic_Morphological_Analysis/data/dictstems_utf8"
    val dsu_file = "/Users/chagerman/Code/Arabic_Morphological_Analysis/data/dictsuffixes_utf8"
    val tab_file = "/Users/chagerman/Code/Arabic_Morphological_Analysis/data/tableAB_utf8"
    val tac_file = "/Users/chagerman/Code/Arabic_Morphological_Analysis/data/tableAC_utf8"
    val tbc_file = "/Users/chagerman/Code/Arabic_Morphological_Analysis/data/tableBC_utf8"
    val (prefix_dict, stem_dict, suffix_dict) = Dicts.get_dicts(dpr_file, dst_file, dsu_file)
    val (tableAB, tableAC, tableBC) = Dicts.get_tables(tab_file, tac_file, tbc_file)
    (prefix_dict, stem_dict, suffix_dict, tableAB, tableAC, tableBC)
}


///////////////          utility methods          ////////////////////
def textToLemmaBOW(text: String, no_solution: Set[String]): String = {
    val (prefix_dict, stem_dict, suffix_dict, tableAB, tableAC, tableBC) = init()
    var lemmaArray: Array[String] = Array()
    val tokens = tokenize(text)
    for(token <- tokens) {
        val translit = transliterate(token)
        val solutions = wordSolutions(translit, prefix_dict, stem_dict, suffix_dict, tableAB, tableBC, tableAC)
        val lemma = solutionLemmas(solutions).mkString(" ")
        if (no_solution.contains(token)) lemmaArray :+ token
        else if (lemma.length > 0)  {
            lemmaArray = lemmaArray :+ lemma
        }
        else lemmaArray = lemmaArray :+ token
    }
    return lemmaArray.mkString(" ")
}


/**
 * For printing out results of transliteration and lemmatization
 */

def textToLemmaSolutions(text: String) = {
    val (prefix_dict, stem_dict, suffix_dict, tableAB, tableAC, tableBC) = init()
    val tokens = tokenize(text)
    for(token <- tokens) {
        val translit = transliterate(token)
        val solutions = wordSolutions(translit, prefix_dict, stem_dict, suffix_dict, tableAB, tableBC, tableAC)
        var lemmas = solutionLemmas(solutions).mkString(",")
        if (lemmas.length == 0) lemmas = "no_solution"
        println("TOKEN:\t" + token + "\tTRANSLITERATION:\t" + translit + "\t" + "\tLemmas:\t" + lemmas)
    }
}

def lemmatize(token: String): List[String] = {
    val (prefix_dict, stem_dict, suffix_dict, tableAB, tableAC, tableBC) = init()
    val translit = transliterate(token.trim)
    val solutions = wordSolutions(translit, prefix_dict, stem_dict, suffix_dict, tableAB, tableBC, tableAC)
    val lemmas = solutionLemmas(solutions)
    lemmas
}

def checkArabic(token: String) = {
    val (prefix_dict, stem_dict, suffix_dict, tableAB, tableAC, tableBC) = init()
    val translit = transliterate(token)
    val solutions = wordSolutions(translit, prefix_dict, stem_dict, suffix_dict, tableAB, tableBC, tableAC)
    val lemmas = solutionLemmas(solutions)
    println(token + "\t" + translit + "\t=>\t" + lemmas.mkString(" "))
}





