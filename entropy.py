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
    def entropy_stat(stats):
        entr = 0
        for item in stats.items():
            # estimate token's prior probability
            ttf = item[1]['ttf']
            prior = ttf/TOTAL_TOKENS

            term_entropy = - prior * math.log(prior, 2)

            entr += term_entropy
        return entr

    f = open('question_entropy.txt', 'a')

    es = ESHelper()

    doc = es.getDocumentsByIds([qid], index_name, doc_type)

    stats = es.statistics4docid(index_name, doc_type, qid, add_fields=['best'])

    body_stats = stats['term_vectors']['body']['terms']
    title_stats = stats['term_vectors']['title']['terms']
    question_stats = CommonUtils.mergeDicts(body_stats, title_stats)

    answers_stats = stats['term_vectors']['answers']['terms']
    best_answer_stats = stats['term_vectors']['best']['terms']

    question_entropy = entropy_stat(question_stats)
    answers_entropy = entropy_stat(answers_stats)
    best_entropy = entropy_stat(best_answer_stats)

    f.write('***Question:\n%s\nQuestion entropy:\n%f\nBest answer:\n%s\n, Best answer entropy:\n%f\n,'
            'Answers:\n%s\n, Answers entropy:\n%f\n'
            % (doc[0]['_source']['title']+' ' + doc[0]['_source']['body'], question_entropy,
               doc[0]['_source']['best'], best_entropy, doc[0]['_source']['answers'], answers_entropy))

    print('***Question:\n%s\nQuestion entropy:\n%f\nBest answer:\n%s\n, Best answer entropy:\n%f\n,'
          'Answers:\n%s\n, Answers entropy:\n%f\n'
          % (doc[0]['_source']['title']+' ' + doc[0]['_source']['body'], question_entropy,
             doc[0]['_source']['best'], best_entropy, doc[0]['_source']['answers'], answers_entropy))
    input()
    f.close()


for i in range(1, 10):
    entropy(i, 'yahoo_', 'qa')


