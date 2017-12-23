

infile = ""
for line in open(infile):
    analyze(line)



def analyze(line):
    tokens = tokenize(line)
    for token in tokens:
        analyzeTokens(token)


def analyzeTokens(token):
    
    # detect non-Arabic words
    non_arabic = re.compile("([\u067E\u0686\u0698\u06AF\u0621-\u063A\u0641-\u0652])+")
    if re.search(non_arabic, token):
        # should split, output subtokens
        print("Non-Arabic : " + token)
    hasSolution = True
    else:
        translitered = romanizeWord(token)
        if translitered in found_dict:          # token has already been processed, solution found
            hasSolution = True
        elif translitered in notFound_dict:     # token has already been processed, solution not found
            hasSolution = False
        else:                                   # not yet processed
            if feedWordSolutions(translitered):   # word has solutions
                hasSolution = True
            else:                                   # no direct solution, check alternatives
                if feedAlternativeSpellings(translitered):
                    # check for solutions using alternative spellings
                    pass





def feedWordSolutions(translitered):
    if translitered in sol:
        return True

    segments = segmentWord(translitered)
    




