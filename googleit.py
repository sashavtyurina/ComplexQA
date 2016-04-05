#ACCOUNT_KEY = '/Mf56CFpuSUUQKdutUsoquPduXPWJBjflVKxHC3GQAk' # sasha.vtyurina account
# ACCOUNT_KEY = 'yxckQrsJG08opmtaZroAT9hMrAiD6CBgxK/NbWYFQns=' # mal.raynolds account
ACCOUNT_KEY = '7ntaEOCRdOwFmQQg3NkTfwv/g+xfo2ZCHnxnMvpFdnQ' # ross.thedivorcer@outlook.com
#ACCOUNT_KEY = 'mTrHvU3N4LLH6yl2RvjAJpkqhRG++wf7Jl4IjU8VN6w' # monica.velula.geller@outlook.com
TOP_DOC = 10
import bing
from os import listdir
from os.path import isfile, join


def ask_bing(query, filename, questID):

    # with open('FullProbes/BingSearchResultsQuest27.txt', 'a') as f:
    with open(filename, 'a') as f:
        # f.write('%s\n' % questID)
        bing_api = bing.Api(ACCOUNT_KEY)
        json_results = bing_api.query(query, source_type='Web', top=TOP_DOC)

        searchResults = []

        for result in json_results:
            snippet = result['Title'] + ' ' + result['Description']
            url = result['Url']
            searchHit = {'snippet' : snippet, 'url': url}
            searchResults.append(searchHit)


        jsonObject = {'query': query, 'snippets': searchResults, 'questID': 27}
        f.write('%s\n' % jsonObject)




def main ():
    mypath = 'TopQuestWordsQueries/'
    onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f))]
    for ff in onlyfiles:
        with open(mypath + ff) as f:
            counter = 0
            for line in f:
                line = line.strip()
                if (line == ''):
                    continue

                print('Working with query %d :: %s' % (counter, line))
                counter += 1

                questID = ff[ff.index('QueriesForQuestion')+len('QueriesForQuestion'):ff.index(".txt")]
                print(questID)
                newFilename = mypath + ff[0:ff.index(".txt")] + "SearchResults.txt"
                ask_bing(line, newFilename, questID)



if __name__ == '__main__':
    main()

