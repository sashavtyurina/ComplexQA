import json

with open('inputQuestionstest.txt') as f:

	for line in f:
		d = eval(line.strip())

		qid = d['id']
		best = d['best']
		all_answers = []

		
		for i in range(0, len(d['answers'])):
			cur_answer = d['answers'][i].replace('\n', '')
			all_answers.appned(cur_answer)

			with open('Answer.%i.%s.yahoo.txt' % (i, qid), 'w'):
				ff.write(cur_answer)

		with open('Answer.Best.%s.yahoo.txt' % qid, 'w') as ff:
			ff.write(best.replace('\n', ''))

		with open('Answer.All.%s.yahoo.txt' % qid, 'w') as ff:
			ff.write(' '.join(all_answers))




