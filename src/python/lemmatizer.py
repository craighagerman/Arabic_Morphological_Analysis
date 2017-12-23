

import re
import sys

from collections import defaultdict

##########################################################################################
#           Tokenization
##########################################################################################

def tokenize(line):
    spc = re.compile(r'''\s+''')
    line = re.sub(spc, ' ', line.strip())
    tatweel = re.compile(r'''\u0640''')         #\u0640 : ARABIC TATWEEL (elongation)
    diacritics = re.compile(r'''[FNKaui~o]''')
    non_arabic = re.compile(r'''([^\u067E\u0686\u0698\u06AF\u0621-\u0636\u0637-\u0643\u0644\u0645-\u0648\u0649-\u064A\u064B-\u064E\u064F\u0650\u0651\u0652]+)''')
    splt = re.split(non_arabic, line)
    return splt


##########################################################################################
#           Buckwalter Transliteration: Arabic -> Romanized
##########################################################################################

def transliterate(word):
    transliteration_table = {
        ord(u"\u0621"): "'",
        ord(u"\u0622"): "|",
        ord(u"\u0623"): ">",
        ord(u"\u0624"): "&",
        ord(u"\u0625"): "<",
        ord(u"\u0626"): "}",
        ord(u"\u0627"): "A",
        ord(u"\u0628"): "b",
        ord(u"\u0629"): "p",
        ord(u"\u062A"): "t",
        ord(u"\u062B"): "v",
        ord(u"\u062C"): "j",
        ord(u"\u062D"): "H",
        ord(u"\u062E"): "x",
        ord(u"\u062F"): "d",
        ord(u"\u0630"): "*",
        ord(u"\u0631"): "r",
        ord(u"\u0632"): "z",
        ord(u"\u0633"): "s",
        ord(u"\u0634"): "$",
        ord(u"\u0635"): "S",
        ord(u"\u0636"): "D",
        ord(u"\u0637"): "T",
        ord(u"\u0638"): "Z",
        ord(u"\u0639"): "E",
        ord(u"\u063A"): "g",
        ord(u"\u0640"): "_",
        ord(u"\u0641"): "f",
        ord(u"\u0642"): "q",
        ord(u"\u0643"): "k",
        ord(u"\u0644"): "l",
        ord(u"\u0645"): "m",
        ord(u"\u0646"): "n",
        ord(u"\u0647"): "h",
        ord(u"\u0648"): "w",
        ord(u"\u0649"): "Y",
        ord(u"\u064A"): "y",
        ord(u"\u064B"): "F",
        ord(u"\u064C"): "N",
        ord(u"\u064D"): "K",
        ord(u"\u064E"): "a",
        ord(u"\u064F"): "u",
        ord(u"\u0650"): "i",
        ord(u"\u0651"): "~",
        ord(u"\u0652"): "o",
        ord(u"\u0670"): "`",
        ord(u"\u0671"): "{",
        ord(u"\u067E"): "P",
        ord(u"\u0686"): "J",
        ord(u"\u06A4"): "V",
        ord(u"\u06AF"): "G"
    }
    return word.translate(transliteration_table)




##########################################################################################
#           Segment word into all all possible
#           (prefix, stem, suffix) possibilities
##########################################################################################

def segment(word):
    ''' (str) -> list of tuples of str
        Segment the input word into all possible prefix, stem, suffix combinations.
            Prefix contains 0-4 characters
            Stem contains 1-infinite characters
            Suffix contains 0-6 characters
        Return a list of (pre, stem, suf) tuples
    '''
    segments = []
    for i in range(min(5, len(word)-1)):
        prefix = word[:i]
        remain = word[i:]
        for j in range(len(remain),  max(len(remain)-7, 0)   , -1):
            suffix = remain[j:]
            stem = remain[:j]
            segments.append( (prefix, stem, suffix) )
    return segments



##########################################################################################
#           Find solutions, get lemmas for word
##########################################################################################

# check if the morphology of pref and stem match. Return boolean
def isCompatible(pref, stem, table):
    pair = " ".join([pref[3], stem[3]])
    print("PAIR:\t{}".format(pair))
    return pair in table
    

def wordSolutions(token, prefix_dict, stem_dict, suffix_dict, tableAB, tableBC, tableAC):
    solutions = []
    segments = segment(token)
    for seg in segments:
        pref, stem, suff = seg
        # check if prefix, stem and suffix are known
        if pref in prefix_dict and stem in stem_dict and suff in suffix_dict:
            # print(pref, stem, suff)

            # check compatibiliity
            prefix_alts = prefix_dict[pref]
            for pa in prefix_alts:
                stem_alts = stem_dict[stem]
                for sta in stem_alts:
                    if isCompatible(pa, sta, tableAB):
                        suffix_alts = suffix_dict[suff]
                        for sfa in suffix_alts:
                            if isCompatible(pa, sfa, tableAC) and isCompatible(sta, sfa, tableBC):
                                solutions.append((pref, stem, suff))

    return set(solutions)



def solutionLemmas(solutions, stem_dict):
    stems = [x[1] for x in solutions]
    # result = []
    # for stem in stems
    #   for entry in stem_dict[stem]:
    #       result.append(entry[1])
    return [entry[1] for stem in stems for entry in stem_dict[stem]]
    # n.b. have to apply .strip() to get rid of newline - should be done in building dict
    # n.b. this gets ALL lemmas for the given stem - we actually just want the particular stem's lemmas.







##########################################################################################
#           Load dict file into hash table
#           key is entry, value is a list (each entry can have multiple values)
##########################################################################################
def load_dict(filename):
    dd = defaultdict(list)
    lemmaID = ""
    print("loading {}".format(filename))
    
    pospat = re.compile("<pos>(.+?)</pos>")
    lemmas = set([])
    lemmaID = "";
    with open(filename) as f:
        for line in f:
            # new lemma
            if (line.startswith(";; ")):
                lemmaID = line[3:]
                lemmas.add(lemmaID);
            # comment
            elif (line.startswith(";")):
                pass
            # entry
            else:
                col = line.split("\t")
                entry = col[0]
                vocalization = col[1]
                morphology = col[2]
                # gloss & POS not relevant for lemmatizing?
                gloss = col[3].split("<pos>")[0].strip()
                match = re.search(pospat, col[3])
                if match:
                    POS = match.group(1)
                else:
                    POS = deducePOS(morphology, vocalization, gloss)
                dd[entry].append([entry, lemmaID, vocalization, morphology, gloss, POS])
    return dd


def deducePOS(morphology, vocalization, gloss):
    # print("MORPHOLOGY:\t{}\nVOCALIZATION:\t{}\nGLOSS:\t{}".format(morphology, vocalization, gloss))
    if re.search("^(Pref-0|Suff-0)$", morphology):
        POS = ""
    elif re.search("^F", morphology):
        POS = vocalization + "/FUNC_WORD"
    elif re.search("^IV", morphology):
        POS = vocalization + "/VERB_IMPERFECT"
    elif re.search("^PV", morphology):
        POS = vocalization + "/VERB_PERFECT"
    elif re.search("^CV", morphology):
        POS = vocalization + "/VERB_IMPERATIVE"
    # educated guess
    elif re.search("^N", morphology):
        if re.search("^[A-Z]", gloss):
            POS = vocalization + "/NOUN_PROP"
        # n.b. some of these are apparently supposed to be ADJ's
        elif re.search("iy~$", vocalization):
            POS = vocalization + "/NOUN"
        else:
            POS = vocalization + "/NOUN"
    return POS



##########################################################################################
#       Compatibility Tables   e.g tableAB
##########################################################################################
def loadCompatibilityTable(tname, filename):
    ''' (str, str) -> set '''
    return set([" ".join(line.strip().split("\t")) for line in open(filename) if not line.startswith(";")])



def main(text, dpr_file, dst_file, dsu_file, tab_file, tac_file, tbc_file):
    # =============== Pipeline ======================
    # Load dictionaries
    # Tokenize words, detect Arabic
    # Transliterate
    # Segment
    # Find solutions
    # Extract lemmas
    # ===============================================

    print("Initializing in-memory dictionary handler...")
    # load 3 lexicons
    prefix_dict = load_dict(dpr_file)
    stem_dict = load_dict(dst_file)
    suffix_dict = load_dict(dsu_file)

    #load 3 compatibility tables
    tableAB = loadCompatibilityTable("tableAB", tab_file )
    tableAC = loadCompatibilityTable("tableAC", tac_file )
    tableBC = loadCompatibilityTable("tableBC", tbc_file )

    token = "wbAlErbyp"
    token = "wSfh"
    solutions = wordSolutions(token, prefix_dict, stem_dict, suffix_dict, tableAB, tableBC, tableAC)



if __name__ == '__main__':
   # define necessary paths
   '''
    dpr_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/dictprefixes_utf8"
    dst_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/dictstems_utf8"
    dsu_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/dictsuffixes_utf8"
    tab_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/tableAB_utf8"
    tac_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/tableAC_utf8"
    tbc_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/tableBC_utf8"
   '''

    # Load text file
    filename = sys.argv[1]
    text = " ".join([line.strip() for line in open(filename)])

    main(dpr_file, dst_file, dsu_file, tab_file, tac_file, tbc_file)



"""
n.b. Should use lazy loading of dictionaries. Also put classes inside dictionaries instead of dictionaries of dictionaries
example:

class Lazywrapper(object):
    def __init__(self, filename):
        self.filename = filename
        self._data = None

    def get_data(self):
        if self._data = None:
            self._build_data()
        return self._data

    def _build_data(self):
        # Now open and iterate over the file to build a datastructure, and
        # put that datastructure as self._data
With the above class you can do this:

puppies = Lazywrapper("puppies.csv") # Instant
kitties = Lazywrapper("kitties.csv") # Instant

print len(puppies.getdata()) # Wait
print puppies.getdata()[32] # instant

"""


