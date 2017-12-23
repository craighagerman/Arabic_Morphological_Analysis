
/*
  Some scripts for processing an Arabic sentiment corpus
  
  PIPELINE
    read ArSenL, create set of neg/pos lemmas
    read E keywords -> lemmatize -> create sets of neg/pos lemmas  (retain words with no solutions)
    read E corpus -> lemmatize (retain keywords in neg/pos sets) -> output as lemmas + tokens
 */
    

import scala.io.Source
import java.io._

:load aramorph.scala



val ER_DIR = "V2-GS-RR-TwitterSentimentCorpus_Ar/"
val OMA_DIR = "OMA-Project/"


def writeLemmas(outpath: String, lexicon: Array[String]) = {
    val file = new File(outpath)
    val bw = new BufferedWriter(new FileWriter(file))
    // output only solution lemmas, ignore words without solution
    // for (lex <- lexicon) bw.write(lemmatize(lex).mkString("\n") + "\n")
    // fall-back on outputting Arabic word if not solution is found
    for (lex <- lexicon) {
        var lemmas = lemmatize(lex)
        if (lemmas.length == 0) lemmas = List(lex)
        bw.write(lemmas.mkString("\n") + "\n")
    }
    bw.close()
}

/*
Read positive / negative \n-delimited lexicon from a file
Return lexicon as an Array
 */
def getLexicon(filename: String): Array[String] = {
    val lexicon = Source.fromFile(filename).getLines.toArray.flatMap(_.split(" "))
    return lexicon
}


def lemmatizeTokens(tokenArray: Array[String]): Set[String] = {
    var tokenSet: Set[String] = Set()
    for (ta <- tokenArray) {
        var lemmas = lemmatize(ta)
        tokenSet = tokenSet ++ lemmas.toSet
    }
    tokenSet
}


// def processTweet(status: Array[String], posSet: Set[String], negSet: Set[String]): Array[String] = {
def processTweet(status: Array[String], no_solution: Set[String]): Array[String] = {
    val (sentiment, tweet_id, text) = (status(0), status(1), status(2))
    val lemmaText = textToLemmaBOW(text, no_solution)
    return Array(sentiment, tweet_id, lemmaText)
}


def lemmatizeTweets(inpath: String, outpath: String, no_solution: Set[String]) = {
    val statuses = Source.fromFile(inpath).getLines.toArray.map(_.split("\t"))
    val file = new File(outpath)
    val bw = new BufferedWriter(new FileWriter(file))
    for (status <- statuses) bw.write(processTweet(status, no_solution).mkString("\t")  + "\n")
    bw.close()
}




val posset = lemmatizeTokens(getLexicon("E-positive.txt"))
val negset = lemmatizeTokens(getLexicon("E-negative.txt"))
writeLemmas("E-pos-UPDATE.txt", posset.toArray)
writeLemmas("E-neg-UPDATE.txt", negset.toArray)


val no_solution = Set("خقاق", "كيوت", "مستانس", "متحسن", "تفاول", "مرفه", "خبال", "فهاوه", "مشتت", "موجع", "ضايع", "مخطى", "خاطى", "تفو", "شوهتوا", "خاين", "طاىفي", "جراىمهم", "اتفو", "موامره", "الموامره", "بايخ", "بايخه", "يخسى", "انقلع", "انقلعوا", "فضايح", "خاطىه", "الجبنا", "بيستغلوا", "بلطجه", "جراىم")

val inpath = "hasSentiment-corpus.tsv"
val outpath = "hasSentiment-corpus-UPDATE.tsv"
lemmatizeTweets(inpath, outpath, no_solution)






// load a set of all possible sentiment lemmas
// Create an array with schema:
//      Aramorph_lemma,  Positive_Sentiment_Score,  Negative_Sentiment_Score,  Confidence
val arsenl_file = OMA_DIR + "ArSenL/ArSenL_v1.0A.txt"
val arsenl = Source.fromFile(arsenl_file).getLines.toArray.filter(!_.startsWith("//")).filter(_.trim != "" ).map(_.split(";")).map(x => Array(x(0), x(2), x(3), x(4)))
val lexicon = arsenl.map(x => x(0)).toSet


////////////////////////////////////////////////////////////////////////////////////////////////////
//                  RE-Write Lexicon As Lemmas
////////////////////////////////////////////////////////////////////////////////////////////////////
// Create a set of lemmas of Rafee's negative words
val er_neg = ER_DIR + "E-ArSubLexicon/E-negative.txt"
val er_pos = ER_DIR + "E-ArSubLexicon/E-positive.txt"


// val neg_lex = getLexicon(er_neg)
// val pos_lex = getLexicon(er_pos)


val er_neg_out = ER_DIR + "E-ArSubLexicon/E-negative-lemmas.txt"
val er_pos_out = ER_DIR + "E-ArSubLexicon/E-positive-lemmas.txt"




// write Rafee's lemmatized lexicon
writeLemmas(er_neg_out, neg_lex)
writeLemmas(er_pos_out, pos_lex)



////////////////////////////////////////////////////////////////////////////////////////////////////
//              Re-Write Corpus As Lemmas
////////////////////////////////////////////////////////////////////////////////////////////////////
// E Rafee's sentiment-annotated Twitter corpus
val er_fulltext_file = ER_DIR + "full-corpus.tsv"
val lines = Source.fromFile(er_fulltext_file).getLines.toArray.map(_.split("\t"))

val inpath = "E-full-corpus.tsv"

// E lexicon
val neg_lex =
val pos_lex =

val neg_lex_tok = getLexicon("E-neg-lem_tok.txt")
val pos_lex_tok = getLexicon("E-pos-lem_tok.txt")
val outpath = "E-lemmas-tokens.tsv"
// ArSenL lexicon
val neg_lex = getLexicon("arsenl-neg.txt")
val pos_lex = getLexicon("arsenl-pos.txt")
val outpath = "ArSenL-lemmas-tokens.tsv"
lemmatizeTweets(inpath, outpath, pos_lex.toSet, neg_lex.toSet)


val sample = Source.fromFile("E-sample-tweets-only.tsv").getLines.toArray
for (text <- sample) textToLemmaSolutions(text)

