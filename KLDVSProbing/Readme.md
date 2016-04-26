Get top 20 words from question by KLD
Compose all possible queries out of them 
Probe Bing with all those queries
Compare retrieved snippets with the question and answers
Rank probes
Rank words from the probes
Compare initial KLD ranking to the resulting ranking after probing


Queries/ - contains all the queries split by the question they were generated from
Snippets/  - contains all the retrieved snippets
AllSearchResults.txt - contains all the snippted cat in single file

 ________
|        |
| v1.txt |
|________|

1. Select top 10 words from question by KLD
2. Construct all possible 3-word queries out of them (120 queries)
3. Probe with each query and get 10 snippets for each
4. Evaluate the "goodness" of the query by comparing it to the question and answers
5. Rank the queries
6. Select separate words out of top-10 ranked queries
7. Rank the words by its frequency in (6)
8. Calculate RPB of initial word ranking (1) and ranking after probing (7)

Results for this are in v1.txt. We see that KLD is almost always better than the probing. 
One possible reason of why this is. 
Given a good query, e.g. "puppy eating gravel", we look at snippets that were retrieved with it and question/answers text. 

	1. How Do You Get Your Dog To Stop Eating Rocks? How Do You Get Your Dog To Stop Eating Rocks? ... This condition is known as ‘pica’, and it can lead to your dog trying to eat rocks, dirt, gravel, ...

	2. Puppy eating gravel | The Labrador Forum Hello! Our 8 week old puppy has discovered the gravel paths around the house and has been trying to eat them. Unfortunately some are small and look just like his food.

	3. Is it safe for my puppy to eat gravel - Answers.com Puppies Eating Gravel Dogs have strange behavior and some of the traits vary from breed to breed. I own a labrador retriever and I know they are.

	4. puppy wont stop eating gravel..? | Yahoo Answers my 8 month old puppy always lickes the ground and tries to eat this blue-ish/gray gravel that lines the paths at the park ... Puppy wont stop eating gravel..?

	5. Why do dogs eat rocks, dirt, gravel, etc? - The Dog Forum Why do dogs eat rocks, dirt, gravel, etc? This is a discussion on Why do dogs eat rocks, dirt, gravel, etc? within the Dog Training and Behavior forums ...

	6. How to Get a Dog to Stop Eating Dirt (with Pictures) - wikiHow Expert Reviewed How to Get a Dog to Stop Eating Dirt. Two Parts: Determining Why Your Dog is Eating Dirt Preventing Your Dog From Eating Dirt Questions and Answers

	7. Why Do Dogs Eat Rocks? - PetPlace.com Why Do Dogs Eat Rocks? Dogs eat a lot of questionable things that, from our point of view, make no sense, such as their own or another animal's stool.

	8. Why is my 17 year old dog eating gravel? | Yahoo Answers At this age your dog would not have any teeth left? I would say that he is eating gravel to assist with digestion. Also, now is the time for a checkup at ...

	9. How do I keep my dog from eating rocks? - Dogs - Dogster How do I keep my dog from eating rocks? Dr. Barchas, We have a three-month-old Border Collie who EATS rocks (gravel). Our back yard is about 1/3 gravel and that is ...

	10. My dog is eating rocks? | Yahoo Answers Best Answer: How old is your dog? i had a puppy who used to eat anything, including gravel. Onw time he managed to chew the back off a remote ...

* we first remove all the query words from the snippets to get:

	1. How Do You Get Your Dog To Stop Rocks? How Do You Get Your Dog To Stop Rocks? ... This condition is known as ‘pica’, and it can lead to your dog trying to eat rocks, dirt,, ...

	2. | The Labrador Forum Hello! Our 8 week old has discovered the paths around the house and has been trying to eat them. Unfortunately some are small and look just like his food.

	3. Is it safe for my to eat - Answers.com Puppies Dogs have strange behavior and some of the traits vary from breed to breed. I own a labrador retriever and I know they are.

	4. wont stop..? | Yahoo Answers my 8 month old always lickes the ground and tries to eat this blue-ish/gray that lines the paths at the park ... wont stop..?

	5. Why do dogs eat rocks, dirt,, etc? - The Dog Forum Why do dogs eat rocks, dirt,, etc? This is a discussion on Why do dogs eat rocks, dirt,, etc? within the Dog Training and Behavior forums ...

	6. How to Get a Dog to Stop Dirt (with Pictures) - wikiHow Expert Reviewed How to Get a Dog to Stop Dirt. Two Parts: Determining Why Your Dog is Dirt Preventing Your Dog From Dirt Questions and Answers

	7. Why Do Dogs Eat Rocks? - PetPlace.com Why Do Dogs Eat Rocks? Dogs eat a lot of questionable things that, from our point of view, make no sense, such as their own or another animal's stool.

	8. Why is my 17 year old dog? | Yahoo Answers At this age your dog would not have any teeth left? I would say that he is to assist with digestion. Also, now is the time for a checkup at ...

	9. How do I keep my dog from rocks? - Dogs - Dogster How do I keep my dog from rocks? Dr. Barchas, We have a three-month-old Border Collie who EATS rocks ). Our back yard is about 1/3 and that is ...

	10. My dog is rocks? | Yahoo Answers Best Answer: How old is your dog? i had a who used to eat anything, including. Onw time he managed to chew the back off a remote ...

** then we look at how many words intersect with top quesstion words and top answer words
Top question words in this case are: puppy, eating, gravel, appetite, pea, pit, bull, mannered, landscaping, wrong. (0 overlap for every snippet)
Top answer words : eat, dog, chew, shouldn't, puppy, vet, teeth, puppies, don't, pup ()
1. 2
2. 1
3. 3
4. 1
5. 2 
6. 1
7. 2
8. 2
9. 1
10. 3
 Giving an overall score of 0.25*0.0 + 0.25*0.0 {question intersection} + 0.25*0.18(average intersection) + 0.25*0.5(overall intersection) = 0.17




