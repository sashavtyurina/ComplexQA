"""
We want to boil down a long verbose question to a short query, that will capture
the initial information need and will produce good results (that contain an answer somewhere)
"""

from elasticsearch import Elasticsearch as ES
from elasticsearch.client import CatClient as cat
import random
import pprint

stop_list = [w.strip() for w in open('stop_words.txt').readlines()]

class ESHelper:
    def __init__(self):
        self.elasticClient = ES()

    def chooseRandomQuestions(self, index_name, n):
        '''
        From a given index choose n random questions.
        We use them to test how well questions to query works.
        :param index_name: name of an index in ES
        :param n: how many questions would you like to pick
        :return: a list of questions
        '''
        total_docs = int(self.elasticClient.count(index=index_name).get('count', -1))
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
            print(single_doc)
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
        es_query['query'] = {'dis_max': {'queries':[{'match':{'title':query_string}}, {'match':{'body':query_string}}]}}
        #print(es_query)
        searchres = self.elasticClient.search(index=index_name, doc_type=doc_type, body=es_query, size=size)
        print('test test again')
        pprint.pprint(searchres['hits']['hits'])
        return searchres['hits']['hits']

    def statistics4docid(self, index_name, doctype, doc_id):
        request_body = {'fields': ['body', 'title'], 'term_statistics': True, 'field_statistics': True}
        response = self.elasticClient.termvectors(index=index_name, doc_type=doctype, id=doc_id, body=request_body)
        pprint.pprint(response)

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
    def getGlobalProbs(words):
        return ""

    @staticmethod
    def getTFIDF(words):
        return ""

eshelper = ESHelper()
eshelper.statistics4docid(index_name='yahoo', doctype='qapair', doc_id=1)
# eshelper.searchQuery('how should i dye my hair', 'yahoo', 'qapair', size=2)
# test_sample = eshelper.chooseRandomQuestions('yahoo', 5)
# print(test_sample)
# eshelper.getDocumentsByIds(test_sample, 'yahoo', 'qapair')


