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


### v1.txt 

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

    4. {deleted. From Yahoo answers} puppy wont stop eating gravel..? | Yahoo Answers my 8 month old puppy always lickes the ground and tries to eat this blue-ish/gray gravel that lines the paths at the park ... Puppy wont stop eating gravel..?

    5. Why do dogs eat rocks, dirt, gravel, etc? - The Dog Forum Why do dogs eat rocks, dirt, gravel, etc? This is a discussion on Why do dogs eat rocks, dirt, gravel, etc? within the Dog Training and Behavior forums ...

    6. How to Get a Dog to Stop Eating Dirt (with Pictures) - wikiHow Expert Reviewed How to Get a Dog to Stop Eating Dirt. Two Parts: Determining Why Your Dog is Eating Dirt Preventing Your Dog From Eating Dirt Questions and Answers

    7. Why Do Dogs Eat Rocks? - PetPlace.com Why Do Dogs Eat Rocks? Dogs eat a lot of questionable things that, from our point of view, make no sense, such as their own or another animal's stool.

    8. {deleted. From Yahoo answers}  Why is my 17 year old dog eating gravel? | Yahoo Answers At this age your dog would not have any teeth left? I would say that he is eating gravel to assist with digestion. Also, now is the time for a checkup at ...

    9. How do I keep my dog from eating rocks? - Dogs - Dogster How do I keep my dog from eating rocks? Dr. Barchas, We have a three-month-old Border Collie who EATS rocks (gravel). Our back yard is about 1/3 gravel and that is ...

    10. {deleted. From Yahoo answers} My dog is eating rocks? | Yahoo Answers Best Answer: How old is your dog? i had a puppy who used to eat anything, including gravel. Onw time he managed to chew the back off a remote ...

* we first remove all the query words from the snippets to get


    1. How Do You Get Your Dog To Stop Rocks? How Do You Get Your Dog To Stop Rocks? ... This condition is known as ‘pica’, and it can lead to your dog trying to eat rocks, dirt,, ...

    2. | The Labrador Forum Hello! Our 8 week old has discovered the paths around the house and has been trying to eat them. Unfortunately some are small and look just like his food.

    3. Is it safe for my to eat - Answers.com Puppies Dogs have strange behavior and some of the traits vary from breed to breed. I own a labrador retriever and I know they are.

    5. Why do dogs eat rocks, dirt,, etc? - The Dog Forum Why do dogs eat rocks, dirt,, etc? This is a discussion on Why do dogs eat rocks, dirt,, etc? within the Dog Training and Behavior forums ...

    6. How to Get a Dog to Stop Dirt (with Pictures) - wikiHow Expert Reviewed How to Get a Dog to Stop Dirt. Two Parts: Determining Why Your Dog is Dirt Preventing Your Dog From Dirt Questions and Answers

    7. Why Do Dogs Eat Rocks? - PetPlace.com Why Do Dogs Eat Rocks? Dogs eat a lot of questionable things that, from our point of view, make no sense, such as their own or another animal's stool.

    9. How do I keep my dog from rocks? - Dogs - Dogster How do I keep my dog from rocks? Dr. Barchas, We have a three-month-old Border Collie who EATS rocks ). Our back yard is about 1/3 and that is ...

* then we look at how many words intersect with top quesstion words and top answer words

Top question words in this case are: puppy, eating, gravel, appetite, pea, pit, bull, mannered, landscaping, wrong. (0 overlap for every snippet)

Top answer words : eat, dog, chew, shouldn't, puppy, vet, teeth, puppies, don't, pup

All question intersection :: []

All answers intersection :: [puppies, eat, dog]

Total score is 0.11


##### Counter example
On the other hand, if we have a bad query "eating pit mannered", these are the snippets:

    1. A Guide to the Stages of Pit Bull Puppy Development Pit bull puppies are warm, cuddly blank slates. Raising a well-mannered ... Stages of Pit Bull Puppy Development: Stage Number One - 1 to 3 Weeks|http://pitbulls.org/article/guide-stages-pit-bull-puppy-development

    2.{This anippet will be removed, as we don't consider pages that contain answers.yahoo in their url} Why is my 5 month old pit bull puppy eating rocks? | Yahoo ... My dog is very active and has a really good appetite. She loves to play and is very well mannered. Lately she has been eating small pea gravel from my ...|https://answers.yahoo.com/question/index?qid=20061112115706AAVPGBB 

    3. Dog Eating Wood: How To Make It Stop | CanineJournal.com How do I Keep My Dog From Eating Wood? The key to keep your dog from eating wood, as with anything you don’t want them to ingest, is to keep it away from them.|http://www.caninejournal.com/dog-eating-wood/

    4. Video dog eating me - grqeysj.ga My dog Noni eating me ... a request or a video of Dog Eat . This might be the most well-mannered dog we've ... a pack of up to 12 pit bull and mixed-breed dogs ...|http://grqeysj.ga/Jc6X.aspx

    5. Letter: Why Don't Pit Bulls Require 'OK' from Neighbors ... ... Why Don't Pit Bulls Require 'OK' from Neighbors ... for such a mild-mannered animal as a chicken? One of my neighbors has several man-eating, pit-bull ...|http://blog.dogsbite.org/2008/09/letter-why-dont-pit-bulls-require-ok.html

    6. Extremely Well Mannered Blue Nose Pitbull Puppy - For Sale Extremely Well Mannered Blue Nose ... stay, come, eat, lets go, look at ... 2016 by Jasminadubb. Find other similar ads by searching the Pit Bull Dogs & Puppies ...|http://www.petclassifieds.us/445578/Extremely-Well-Mannered-Blue-Nose-Pitbull-Puppy.html

    7. Dealing With a Cat Who Goes Crazy for Food - Petful Dealing With a Cat Who Goes Crazy for Food ... mild-mannered Hungry Henry turns into Church from ... Why suddenly his stomach seems to be a bottomless pit, ...|http://www.petful.com/behaviors/dealing-cat-goes-crazy-food/

    8. How To Eat Dog Shit: A Condiments Guide - Deadspin Greg: OK, so the pit bull mafia gets a hold of you and forces you to eat pit bull poop (let's say an average size dog poo, 3 inches or so). They may be angry, but ...|http://deadspin.com/5921373/what-condiment-goes-best-with-dog-shit

    9. How to Care for an American Staffordshire Terrier | The ... Although this breed is different than the American pit bull ... How to Care for an American Staffordshire Terrier. By ... bull can be well-behaved and well-mannered.|http://www.dailypuppy.com/articles/how-to-care-for-an-american-staffordshire-terrier_1447.html

    10. PitBull Puppies - Getting Started Raising Them Right - Pit ... Getting Started Raising Them Right. ... too much attention and not enough of the other needs results in bad mannered dogs. ... Pit Bulls make excellent detection dogs ...|http://www.pitbulllovers.com/training-articles/pitbull-puppies-teaching-manners.html

* we then remove probe words: eating pit mannered

    1. A Guide to the Stages of Bull Puppy Development bull puppies are warm, cuddly blank slates. Raising a well ... Stages of Bull Puppy Development: Stage Number One - 1 to 3 Weeks

    3. Dog  Wood: How To Make It Stop | CanineJournal.com How do I Keep My Dog From  Wood? The key to keep your dog from  wood, as with anything you don’t want them to ingest, is to keep it away from them.

    4. Video dog  me - grqeysj.ga My dog Noni  me ... a request or a video of Dog Eat . This might be the most well dog we've ... a pack of up to 12 bull and mixed-breed dogs ...

    5. Letter: Why Don't Bulls Require 'OK' from Neighbors ... ... Why Don't Bulls Require 'OK' from Neighbors ... for such a mild animal as a chicken? One of my neighbors has several,-bull ...

    6. Extremely Well Blue Nosebull Puppy - For Sale Extremely Well Blue Nose ... stay, come, eat, lets go, look at ... 2016 by Jasminadubb. Find other similar ads by searching the Bull Dogs & Puppies ...

    7. Dealing With a Cat Who Goes Crazy for Food - Petful Dealing With a Cat Who Goes Crazy for Food ... mild Hungry Henry turns into Church from ... Why suddenly his stomach seems to be a bottomless, ...

    8. How To Eat Dog Shit: A Condiments Guide - Deadspin Greg: OK, so the bull mafia gets a hold of you and forces you to eat bull poop (let's say an average size dog poo, 3 inches or so). They may be angry, but ...

    9. How to Care for an American Staffordshire Terrier | The ... Although this breed is different than the American bull ... How to Care for an American Staffordshire Terrier. By ... bull can be well-behaved and well.

    10. Bull Puppies - Getting Started Raising Them Right - ... Getting Started Raising Them Right. ... too much attention and not enough of the other needs results in bad dogs. ... Bulls make excellent detection dogs ...

* Then we look at how many words from these snippets intersect with our question and answers:

All question intersection :: [bull, puppy]

All answers intersection :: [puppies, eat, don't, puppy, dog]

Total score ::  0.23


### v3_fixedYahooQIDS.txt

Before we would filter out a snippet if it came from Yahoo answers website (i.e. if url contains answers.yahoo.). We did that to eliminate the question itself being retrived. 
But that way some pages from Yahoo answers were filtered out, even though that was a different question. In this version we also keep track of question ids, so we can do a better job filtering out the snippets coming from the questions themselves. 


Also, when evaluating probes and comparing them to snippets, we exteneded the list pf top question words and top answer words. We used to have 10 words in each list, and now it's 20. The reason behind this is that some of the relevant to the question words sometines don't make it to the top-10 list. Therefore, when we get a good snippet, it doesn't get a good score, because the intersection with the question/answer top-10 words is low. 

The result files are v3_fixedYahooQIDS.txt and v3_fixedYahooQIDS.png


### v2.txt

Before we made two changes: extended the list of top words from 10 to 20, and fixed the filtering out of yahoo answer pages. After making these changes, the results seem to have become better. In this version we again use top-10 words from question/answers to evaluate the goodness of the queries. We want to make see which of the above changes made the results better. 

The results for v2 are approximately the same (or a tiny bit better) as in v1. Which tells us that the increase that we got in v3 is coming from the expansion of the top words list from 10 to 20 words.



