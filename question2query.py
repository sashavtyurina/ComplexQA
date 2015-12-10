"""
We want to boil down a long verbose question to a short query, that will capture
the initial information need and will produce good results (that contain an answer somewhere)
"""

from elasticsearch import Elasticsearch as ES
from elasticsearch.client import CatClient as cat
import random
import pprint
import operator
from collections import Counter

stop_list = [w.strip() for w in open('stop_words.txt').readlines()]

class ESHelper:
    def __init__(self):
        self.elasticClient = ES()

    def documentCount(self, index_name):
        total_docs = int(self.elasticClient.count(index=index_name).get('count', -1))
        return total_docs

    def chooseRandomQuestionIds(self, index_name, n):
        '''
        From a given index choose n random questions.
        We use them to test how well questions to query works.
        :param index_name: name of an index in ES
        :param n: how many questions would you like to pick
        :return: a list of question ids
        '''
        # total_docs = int(self.elasticClient.count(index=index_name).get('count', -1))
        total_docs = self.documentCount(index_name)
        if total_docs == -1:
            print('Could not fetch document count. Failed.')
            return

        test_sample = random.sample(range(1, total_docs + 1), n)
        return test_sample

    def getDocumentsByIds(self, ids, index_name='yahoo', doc_type='qapair'):
        '''
        Given a list of ids, find them in the index and return the corresponding records.
        :param ids: id of a document
        :param index_name: the name of the index('yahoo' for WebScopeL6 )
        :param doc_type: the type of the record ('qapair' for WebScopeL6)
        :return: a list of the documents with the corresponding ids.
        '''

        documents = []
        for doc_id in ids:
            single_doc = self.elasticClient.get(index=index_name, doc_type=doc_type, id=doc_id)
            documents.append(single_doc)
            # print(single_doc)
        return documents

    def searchQuery(self, query_string, index_name, doc_type, size=10):
        '''
        Given a query string, this function searches for it in titles and bodies of the questions
        Returns a list of hits. Every hit is a json object with the fields: _id, _index, _score, _source,
        _type. Where _source is a json object with the fields: title, body, answers.
        :param query_string: the query that you would like to look for
        :param index_name: the name of te index. For Yahoo WebScopeL6 the name is 'yahoo'
        :param doc_type: the doc_type of the records in the index. For WebScopeL6 doc_type= "qapair"
        :param size: how many results would you like to get
        :return: returns a list of hits. See the function's description for details.
        '''

        es_query = {}
        es_query['query'] = {'dis_max': {'queries': [{'match': {'title': query_string}}, {'match': {'body': query_string}}]}}
        #print(es_query)
        searchres = self.elasticClient.search(index=index_name, doc_type=doc_type, body=es_query, size=size)
        print('test test again')
        pprint.pprint(searchres['hits']['hits'])
        return searchres['hits']['hits']

    def statistics4docid(self, index_name, doctype, doc_id):
        request_body = {'fields': ['body', 'title'], 'term_statistics': True, 'field_statistics': False,
                        'positions': False, 'offsets': False}
        response = self.elasticClient.termvectors(index=index_name, doc_type=doctype, id=doc_id, body=request_body)
        # pprint.pprint(response)
        return response

    def totalTokensInCorpus(self, index_name, doctype):
        # starting with id of 2 (cause i screwed up the first one) we access it's title and body,
        # claculate the length of the document in terms and ad it up to find the length of the corpus in the end

        total_docs = self.documentCount(index_name)
        print('Total docs is %d' %total_docs)
        total_tokens = 0
        for doc_id in range(2, total_docs):
            try:
                doc_stats = self.statistics4docid(index_name, doctype, doc_id)

                
                body_stats = doc_stats['term_vectors']['body']['terms']
                title_stats = doc_stats['term_vectors']['title']['terms']
                merged_stats = CommonUtils.mergeDicts(body_stats, title_stats)

                doc_length = sum([item[1]['term_freq'] for item in merged_stats.items()])
                # for item in merged_stats.items():
                #     print(str(item))
                #     input()

                #     tf = item[1]['term_freq']
                #     doc_length += tf
                total_tokens += doc_length
            except Exception as e:
                print(str(e))
                print('Currently total tokens number is %d' % total_tokens)
                print('Current doc id is %d' % doc_id)
                continue


        print(total_tokens)
        return total_tokens



class IRUtils:
    @staticmethod
    def text2dict(text_list):
        '''
        Given a list return a dictionary with keys - unique word tokens, values - their frequencies
        :param text: list of tokens
        :return: dictionary
        '''
        final_dict = {}
        for w in text_list:
            final_dict[w]= final_dict.get(w, 0) + 1
        return final_dict

    @staticmethod
    def tokenize(text):
        '''
        Tokenize string text
        :param text: a string of text
        :return: a list of tokens
        '''
        # TODO: add cleaning, removing strange symbols and so on
        from nltk import tokenize
        return tokenize.word_tokenize(text)

    @staticmethod
    def removeStop(text_list):
        '''
        Given a list of words removes stop words from it. Returns a clean list.
        :param text_list: initial list of tokens
        :return: Cleaned list of tokens. No stop words.
        '''
        clean_text = [w for w in text_list if w not in stop_list]
        return clean_text

    @staticmethod
    def removeStopDict(textDict):
        '''
        Given a dictionary with words as keys remove those pairs that have a stop word as their key
        :param textDict: input dictionary. Has words as keys. Whatever else as values
        :return: Clean dictionary
        '''

        clean_dict = {}
        for word in textDict.keys():
            if word in stop_list:
                continue
            clean_dict[word] = textDict[word]
        return clean_dict

    @staticmethod
    def getGlobalProbs(words):
        return ""

    @staticmethod
    def getTFIDF(words):
        return ""

class CommonUtils:
    @staticmethod
    def mergeDicts(a, b):
        '''
        Merges dictionaries returned for every document by Elastic API.
        :param a: dict 1
        :param b: dict 2
        :return: adds up all the values, returns the result
        '''

        result = {}
        all_stats = list(a.items()) + list(b.items())
        for term_stat in all_stats:
            word = term_stat[0]
            # if word in stop_list:
            #     continue
            word_ttf = term_stat[1]['ttf']
            word_local_freq = term_stat[1]['term_freq']
            word_doc_freq = term_stat[1]['doc_freq']
            if word in result.keys():
                result[word]['ttf'] += word_ttf
                result[word]['term_freq'] += word_local_freq
                result[word]['doc_freq'] += word_doc_freq
            else:
                result[word] = term_stat[1]
        return result

eshelper = ESHelper()
q_num = 1
index_name = 'yahoo'
doc_type = 'qapair'
total_tokens = eshelper.totalTokensInCorpus(index_name, doc_type)
print('Total tokens in corpus is: %d ' % total_tokens)
input()

question_ids = eshelper.chooseRandomQuestionIds(index_name, q_num)

qapairs = eshelper.getDocumentsByIds(question_ids, index_name, doc_type)
#print(str(qapairs))

i = 0
for pair in qapairs:
    i += 1
    print('Processing question #%d' % i)
    body = pair['_source']['body']
    title = pair['_source']['title']
    answers = pair['_source']['answers']

    print(title)
    print(body)
    stats = eshelper.statistics4docid(index_name, doc_type, pair['_id'])
    # pprint.pprint(str(stats))

    # merge statistics for title and body.
    # We want to have a single dictionary with all the values
    body_stats = stats['term_vectors']['body']['terms']
    title_stats = stats['term_vectors']['title']['terms']

    merged_stats = CommonUtils.mergeDicts(body_stats, title_stats)
    merged_stats = IRUtils.removeStopDict(merged_stats)
    print(type(merged_stats.items()))
    print(merged_stats.items())

    tfidfs = [(item[0], item[1]['ttf']/item[1]['doc_freq']) for item in merged_stats.items()]
    for ii in sorted(tfidfs, key=operator.itemgetter(1), reverse=True):
        print(str(ii))

    #print(str(merged_stats))




# eshelper.statistics4docid(index_name='yahoo', doctype='qapair', doc_id=1)
# eshelper.searchQuery('how should i dye my hair', 'yahoo', 'qapair', size=2)
# test_sample = eshelper.chooseRandomQuestions('yahoo', 5)
# print(test_sample)
# eshelper.getDocumentsByIds(test_sample, 'yahoo', 'qapair')

  
