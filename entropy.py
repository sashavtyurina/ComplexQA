__author__ = 'Alex'
import math
'''
The corpus WebScope L6 is indexed with Elastic search and is on my lab machine.

Working with Gaurav on finding comprehensiveness of the desired answer.
We have already built a graphs with lengths distributions, but they didn't seem to give much insight.
The next step is to look at entropy distributions. And this is what I'm going to do.
'''
from question2query import ESHelper, TOTAL_TOKENS, CommonUtils

def entropy(qid, index_name, doc_type):
    es = ESHelper()
    stats = es.statistics4docid(index_name, doc_type, qid, add_fields=['best'])

    body_stats = stats['term_vectors']['body']['terms']
    title_stats = stats['term_vectors']['title']['terms']
    question_stats = CommonUtils.mergeDicts(body_stats, title_stats)

    answers_stats = stats['term_vectors']['answers']['terms']

    best_answer_stats = stats['term_vectors']['best']['terms']

    question_entropy = 0
    for item in question_stats.items():
        # estimate token's prior probability
        term = item[0]
        ttf = item[1]['ttf']
        prior = ttf/TOTAL_TOKENS

        term_entropy = - prior * math.log(prior, 2)

        question_entropy += term_entropy



