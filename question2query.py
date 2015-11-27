"""
We want to boil down a long verbose question to a short query, that will capture
the initial information need and will produce good results (that contain an answer somewhere)
"""

from elasticsearch import Elasticsearch as ES
from elasticsearch.client import CatClient as cat

def chooseRandomQuestions(index_name, n):
    '''
    From a given index choose n random questions.
    We use them to test how well questions to query works.
    :param index_name: name of an index in ES
    :param n: how many questions would you like to pick
    :return: a list of questions
    '''
    index_name = 'yahoo'

    # get index length in documents - L
    # produce n random numbers in range 1:L
    # return either a list of ids or a list of actual questions

    total_docs = cat.count(index=index_name, v=True)
    print(type(total_docs))
    print(total_docs)


chooseRandomQuestions('yahoo', 100)