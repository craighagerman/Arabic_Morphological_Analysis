
import dictEntry
import re

from collections import defaultdict


##########################################################################################
#       Dictionary files   e.g. dictsuffix
##########################################################################################
def loadDictionary(filename):
    print("loading {}".format(filename))
    dd = dict()
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
                de = dictEntry.DictionaryEntry(entry, lemmaID, vocalization, morphology, gloss, POS)
                dd[entry] = de
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
    return set([" ".join(line.strip().split("\t")) for line in open(filename) if not line.startswith("")])




###########################################################################################
#     Buckwalker's method
###########################################################################################
# loads a dict file into a hash table where the key is $entry and its value is a list (each $entry can have multiple values)
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



def main():
    # ToDo: have to define input files as below
    '''
    dpr_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/dictprefixes_utf8"
    dst_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/dictstems_utf8"
    dsu_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/dictsuffixes_utf8"
    tab_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/tableAB_utf8"
    tac_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/tableAC_utf8"
    tbc_file = "/Users/chagerman/3rd_party/LDC/buckwalter_morphan_1/data/tableBC_utf8"
    '''
    print("Initializing in-memory dictionary handler...")
    # load 3 lexicons
    dictPrefixes = loadDictionary(dpr_file)
    dictStems = loadDictionary(dst_file)
    dictSuffixes = loadDictionary(dsu_file)

    #load 3 compatibility tables
    hash_AB = loadCompatibilityTable("tableAB", tab_file )
    hash_AC = loadCompatibilityTable("tableAC", tac_file )
    hash_BC = loadCompatibilityTable("tableBC", tbc_file )




if __name__ == '__main__':
    main()
