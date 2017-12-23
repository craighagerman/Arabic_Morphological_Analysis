





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


def print_segments(segments):
    word_len = len(segments[0][1])
    print("="*80)
    print("PREFIX  STEM   SUFFIX")
    for seg in segments:
        print("{: <6} {: <{}} {: <6}".format(seg[0], seg[1], word_len+1, seg[2]))

##########################################################################################





def x():
    "abcabc".translate({ord('a'): 'd', ord('c'): 'x'})
    #'dbxdbx'

# Buckwalter transliteration
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
    # "abcabc".translate({ord('a'): 'd', ord('c'): 'x'})
    return word.translate(transliteration_table)



def tokenize(line):
    spc = re.compile(r'''\s+''')
    line = re.sub(spc, ' ', line.strip())
    tatweel = re.compile(r'''\u0640''')         #\u0640 : ARABIC TATWEEL (elongation)
    diacritics = re.compile(r'''[FNKaui~o]''')
    non_arabic = re.compile(r'''([^\u067E\u0686\u0698\u06AF\u0621-\u0636\u0637-\u0643\u0644\u0645-\u0648\u0649-\u064A\u064B-\u064E\u064F\u0650\u0651\u0652]+)''')
    splt = re.split(non_arabic, line)
    return splt


##########################################################################################


def main():
    word = input("Enter a transliterated word: ").strip()
    if word == '':
        print("Here is an example for `wbAlErbyp`")
        word = "wbAlErbyp"
    segments = segment(word)
    print_segments(segments)






if __name__ == '__main__':
    # USAGE:
    # python3 arabic_word_segmenter.py
    main()
