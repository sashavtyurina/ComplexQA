"""
We want to boil down a long verbose question to a short query, that will capture
the initial information need and will produce good results (that contain an answer somewhere)
"""

from elasticsearch import Elasticsearch as ES
from elasticsearch.client import CatClient as cat
import random
import pprint


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

        # get index length in documents - L
        # produce n random numbers in range 1:L
        # return either a list of ids or a list of actual questions
        elastic = ES()

        # total_docs = int(elastic.count(index=index_name).get('count', -1))
        total_docs = int(self.elasticClient.count(index=index_name).get('count', -1))
        if total_docs == -1:
            print('Could not fetch document count. Failed.')
            return

        test_sample = random.sample(range(1, total_docs + 1), n)
        return test_sample

    def getDocumentsByIds(self, ids, index_name='yahoo', doc_type='qapair'):
        for doc_id in ids:
            single_doc = self.elasticClient.get(index=index_name, doc_type=doc_type, id=doc_id)
            print(single_doc)

    def searchQuery(self, query_string, index_name, doc_type, size=10):
      es_query = {}
      es_query['query'] = {'dis_max': {'queries':[{'match':{'title':query_string}}, {'match':{'body':query_string}}]}}
      #print(es_query)
      searchres = self.elasticClient.search(index=index_name, doc_type=doc_type, body=es_query, size=size)
      print('test test')
      pprint.pprint(searchres)
      return searches
eshelper = ESHelper()
eshelper.searchQuery('how should i dye my hair', 'yahoo', 'qapair', size=2)
#test_sample = eshelper.chooseRandomQuestions('yahoo', 5)
#print(test_sample)
#eshelper.getDocumentsByIds(test_sample, 'yahoo', 'qapair')


