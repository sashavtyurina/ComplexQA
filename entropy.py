__author__ = 'Alex'
import math
from nltk import tokenize, FreqDist
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

    def entropy_answers(answers, answers_stats):
        '''

        :param answers: list of answers
        :param answers_stats: stats for answers field
        :return:
        '''

        result = {}
        for a in answers:
            cur_answer_stats = {}
            tokens = [t.lower() for t in tokenize.word_tokenize(a)]
            for t in tokens:
                try:
                    if t in answers_stats.keys():
                        cur_answer_stats[t] = answers_stats[t]
                except Exception as e:
                    print(str(e))
            cur_answer_entropy = entropy_stat(cur_answer_stats)
            result[a] = cur_answer_entropy
        return result


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
    answers_entropy = entropy_answers(answers_stats)
    best_entropy = entropy_stat(best_answer_stats)

    f.write('***Question:\n%s\nQuestion entropy:\n%f\nBest answer:\n%s\n, Best answer entropy:\n%f\n,'
            'Answers:\n%s\n, Answers entropy:\n%s\n'
            % (doc[0]['_source']['title']+' ' + doc[0]['_source']['body'], question_entropy,
               doc[0]['_source']['best'], best_entropy, doc[0]['_source']['answers'], str(answers_entropy)))

    print('***Question:\n%s\nQuestion entropy:\n%f\nBest answer:\n%s\n, Best answer entropy:\n%f\n,'
          'Answers:\n%s\n, Answers entropy:\n%s\n'
          % (doc[0]['_source']['title']+' ' + doc[0]['_source']['body'], question_entropy,
             doc[0]['_source']['best'], best_entropy, doc[0]['_source']['answers'], str(answers_entropy)))
    input()
    f.close()

def entropy1(qid, index_name, doc_type):
    def build_freq_dist(stats):
        freq_dist = {}
        length = len(stats.items())
        for item in stats.items():
            freq_dist[item[0]] = item[1]['term_freq']/length
        return freq_dist

    es = ESHelper()
    doc = es.getDocumentsByIds([qid], index_name, doc_type)
    title = doc[0]['_source']['title']
    body = doc[0]['_source']['body']
    best = doc[0]['_source']['best']
    answers = doc[0]['_source']['answers']
    stats = es.statistics4docid(index_name, doc_type, qid, add_fields=['best'])

    body_stats = stats['term_vectors']['body']['terms']
    title_stats = stats['term_vectors']['title']['terms']
    question_stats = CommonUtils.mergeDicts(body_stats, title_stats)
    question_freq_dist = build_freq_dist(question_stats)
    question_entropy = -sum(p * math.log(p, 2) for p in list(question_freq_dist.values()))

    best_answer_stats = stats['term_vectors']['best']['terms']
    best_answer_freq_dist = build_freq_dist(best_answer_stats)
    best_answer_entropy = -sum(p * math.log(p, 2) for p in list(best_answer_freq_dist.values()))

    # we want to use elastic search tokenization, hence the trouble
    answers_stats = stats['term_vectors']['answers']['terms']
    # NOTE! freq dist for answers is a list of dictionaries
    answers_freq_dist = []
    answers_entropy = {}
    for a in answers:
        tokens = [t.lower() for t in tokenize.word_tokenize(a)]
        tokens_clean = [t for t in tokens if t in answers_stats.keys()]
        answers_freq_dist.append(FreqDist(tokens_clean))
        probs = [answers_freq_dist.freq(l) for l in answers_freq_dist]
        answers_entropy[a] = -sum(p * math.log(p, 2) for p in probs)

    print('Question: %s ' % title + ' ' + body)
    print('Question entropy: %f' % question_entropy)

    print('Best answer: %s ' & best)
    print('Best answer entropy: %f' % best_answer_entropy)

    for i in answers_entropy.items():
        print('Answer: %s' % i[0])
        print('Answer\'s entropy: %f' % i[1])

    input()




for i in range(1, 10):
    entropy1(i, 'yahoo_', 'qa')


