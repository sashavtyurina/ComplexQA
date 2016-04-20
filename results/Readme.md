### This is to keep track of the files with the results

* ###### AnswersIntersection.html

  Out of all give answers, what are the words that appear in all of them
  
* ###### CompareWeights.txt

  1. Extract keywords from the question (with reweighting!)
  2. Make sure all the words from the true query are in there 
  3. Construct all possible queries out of those keywords
  4. For each query we have 10 snippets
  5. Compare those snippets to the question + to the answer. 
  Here we change the weights of sim_score between snippet and answer, snippet and question. 

* ###### Keywords.txt

  Extract keywords from questions with reweighting. 
  1. Sort by KLD score at first
  2. Increase scores of words appearing in the title by 1.5
  3. Increase scores of words appearing in question sentences by 1.5
  
* ###### TopWordsIntersectionFullProbes.txt

  1. Compose all possible probes for questions
  2. Retrieve their snippets from Bing
  3. Compare the snippets with question + answer
  4. Rank the probes 
