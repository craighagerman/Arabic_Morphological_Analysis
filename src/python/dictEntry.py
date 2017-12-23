

import re

class DictionaryEntry:
    def __init__(self, entry, lemmaID, vocalization, morphology, gloss, POS):
        ''' (DictionaryEntry, str, str, str, str, str, str) -> NoneType '''
        self.entry = entry.strip();
        self.lemmaID = lemmaID.strip();
        self.vocalization = vocalization.strip();
        self.morphology = morphology.strip();
        self.gloss = gloss;
        self.glosses = []
        self.POS = []
        ''' strip <pos> tags, split on '+' characters
        IN:     <pos>fa/CONJ+sa/FUT+yu/IV3MS+</pos>
        OUT:    ['<pos>fa/CONJ', 'sa/FUT', 'yu/IV3MS', '</pos>']
        '''
        pat = re.compile(r"<pos>(.*)</pos>")
        match = re.search(pat, gloss)
        if match:
            self.glosses = match.group().split("+")
        
            
        match2 = re.search(pat, POS)
        if match2:
            self.POS = match2.group().split("+")
        
    def getEntry():
        ''' () -> str '''
        return self.entry

    def getLemmaID():
        ''' () -> str '''
        return self.lemmaID

    def getVocalization():
        ''' () -> str '''
        return self.vocalization

    def getMorphology():
        ''' () -> str '''
        return self.morphology

    def getPOS():
        ''' () -> list of str '''
        return self.POS

    def getGloss():
        ''' () -> str '''
        return self.gloss

    def getGlosses():
        ''' () -> list of str '''
        return self.glosses

