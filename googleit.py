#ACCOUNT_KEY = '/Mf56CFpuSUUQKdutUsoquPduXPWJBjflVKxHC3GQAk' # sasha.vtyurina account
ACCOUNT_KEY = 'yxckQrsJG08opmtaZroAT9hMrAiD6CBgxK/NbWYFQns=' # mal.raynolds account
#ACCOUNT_KEY = '7ntaEOCRdOwFmQQg3NkTfwv/g+xfo2ZCHnxnMvpFdnQ' # ross.thedivorcer@outlook.com
#ACCOUNT_KEY = 'mTrHvU3N4LLH6yl2RvjAJpkqhRG++wf7Jl4IjU8VN6w' # monica.velula.geller@outlook.com
TOP_DOC = 10
import bing


def ask_bing(query):

    with open('FullProbes/BingSearchResultsQuest27.txt', 'a') as f:
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
    with open('FullProbes/QueriesFirQuestion27.txt') as f:
        counter = 0
        for line in f:
            line = line.strip()
            if (line == ''):
                continue

            print('Working with query %d :: %s' % (counter, line))
            counter += 1

            ask_bing(line)



if __name__ == '__main__':
    main()

