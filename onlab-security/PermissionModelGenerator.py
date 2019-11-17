import urllib2
import urllib
import requests
from bs4 import BeautifulSoup as bs
import codecs
import re
import pymysql
import base64
import nltk
from nltk import chunk
from nltk.chunk import RegexpParser
import inflection
from nltk.corpus import wordnet
from nltk import FreqDist
from treelib import Node, Tree

class makePermission:

	vdist = FreqDist()
	ndist = FreqDist()
	pdist = FreqDist()

	onos_read_verb = [
	'Return',
	'Obtain',
	'Fetch',
	'Get',
	'Indicate',
	'Find',
	'Evaluate',
	'Judge',
	'Retrieve',
	'Retriefe',
	'Check',
	'Provide',
	'Signify',
	'Request',
	'Borrow',
	'Generate',
	'Synchronou']

	onos_write_verb = [
	'Mark',
	'Multicast',
	'Make',
	'assign',
	'Release',
	'Accept',
	'Submit',
	'Broadcast',
	'Cancel',
	'Change',
	'Delete',
	'Destroy',
	'Form',
	'Emit',
	'Purge',
	'Send',
	'Set',
	'Install',
	'Update',
	'Withdraw',
	'Apply',
	'Clear',
	'Remove',
	'Create',
	'Register',
	'Add',
	'Unregister',
	'allocate',
	'Append',
	'Balance',
	'Bind',
	'Notify',
	'Preset',
	'Record',
	'Uninstall']

	onos_execute_verb = [
	'Perform',
	'Stop',
	'Trigger',
	'Enter',
	'Invoke',
	'Abandon',
	'Activate',
	'Deactivate']

	tree = Tree()#
	tree.create_node("Root","ROOT") #

	def insertDB(self,controller,interface,implementation,description,permission):
		conn = pymysql.connect(host='localhost', user='root', password='root', db=controller, charset='utf8')
		curs = conn.cursor()
		sql = """insert into permission (Interface, Implementation, Description, Permission) values (%s, %s, %s, %s)"""
		curs.execute(sql,(interface, implementation, description, permission))
		conn.commit()
		rows = curs.fetchall()
		print (rows)
		conn.close()

		
	def getOnosDesc(self):
		rootNode = self.tree.get_node("ROOT")#
		url='http://api.onosproject.org/1.8.0/'
		mainPage = 'allclasses-frame.html'
		mainDoc = urllib2.urlopen(url+mainPage)
		soup = bs(mainDoc.read(),"lxml")
		f1 = open('/home/sdn/onos.text', 'w+')
		packageDiv = soup.find('div', {"class": "indexContainer"})
		printNum = 1
		methodNum = 1
		allClassesLink = packageDiv.findAll('a')
		for classLink in allClassesLink:
			classTemp = classLink.contents[0]
			classHref =''
			if str(classTemp).startswith('<span') is True:
				className = classTemp.contents[0]
			else:
				className = classTemp

			if str(className).endswith('Service') is True:
				if "Provider" not in str(className):
					classHref = classLink.get('href')
					if "incubator" in classHref:
						continue
					if "onlab" in classHref:
						continue
					if "cpman" in classHref:
						continue

					curNode = rootNode#

					interfaceDoc = urllib2.urlopen(url+classHref)
					soup2 = bs(interfaceDoc.read(),"lxml")
					subTitle = soup2.find('div', {"class":"subTitle"}).contents[0]
					
					if soup2.find('h2',{"class":"title"}).contents[0].startswith('Class') is True:
						continue
					else:
						title = soup2.find('h2',{"class":"title"}).contents[0].split('Interface ')[1]
						print "Title : " + title
						interfaceLocation = subTitle+"."+title
						f1.write("["+str(printNum)+"] API Package path : "+interfaceLocation+"\n")
						printNum+=1
						print "API: "+interfaceLocation

						assetsString = subTitle.split("org.onosproject.")#
						assets = assetsString[1].split(".")#
						for i in range(len(assets)):#
							if self.tree.contains(assets[i]) is True:#
								curNode = self.tree.get_node(assets[i])
								continue#
							else:#
								curNode = self.tree.create_node(assets[i],assets[i],parent=curNode.identifier)#

						methodSummary = soup2.find('table',{"summary":"Method Summary table, listing methods, and an explanation"})
						if methodSummary is not None:
							allTr = methodSummary.findAll('tr')
							if len(allTr) is not 0:
								for tr in allTr:
									allTd = tr.findAll("td")
									if len(allTd) is not 0:
										methodName = allTd[1].find('span',{"class":"memberNameLink"}).contents[0].contents[0]
										descriptionDiv = allTd[1].find('div',{"class":"block"})
										if descriptionDiv is not None:
											deprecatedLabel = descriptionDiv.find('span',{"class":"deprecatedLabel"})
											if deprecatedLabel is not None:
												description=deprecatedLabel.contents[0]
											else:
												description = descriptionDiv.contents[0]
											#description = description.rstrip('\n')
											if description.count(':') is not 0:
												description = description.split(":")
												description = description[0]
											Ndesc = ''
											descTemp = description.split()
											for i in range(0,len(descTemp)):
										
												Ndesc += descTemp[i] + " "
											print Ndesc
											f1.write("{"+str(methodNum)+"}"+"- ["+methodName+"]"+"\n")
											methodNum=methodNum+1
											f1.write("Description : " + Ndesc + "\n")
											permission = self.semanticParsing2(Ndesc,f1,0)
											permissionSplit = permission.split("_")#
											if len(permissionSplit) is 1:
												permission = self.semanticParsing2(Ndesc,f1,1)
												permissionSplit = permission.split("_")#
											verb = permissionSplit[0]#
											asset = ""#
											if len(permissionSplit) is 1:
												asset = "None"
											else:
												for i in range (1,len(permissionSplit)):#
													assetTemp = permissionSplit[i]#
													if ("set" in assetTemp):
														assetTemp = assetTemp.replace("set","")
													if ("list" in assetTemp) and (assetTemp is not "listener"):
														assetTemp = assetTemp.replace("list","")
													if "collection" in assetTemp:
														assetTemp = assetTemp.replace("collection","")
													if "infrastructure" in assetTemp:
														assetTemp = assetTemp.replace("infrastructure","")
													if "factory" in assetTemp:
														assetTemp = assetTemp.replace("factory","")
													if "summary" in assetTemp:
														assetTemp = assetTemp.replace("summary","")
													if "total" in assetTemp:
														assetTemp = assetTemp.replace("total","")
													if assetTemp == "":
														assets=asset.rstrip('_')
														continue
													if i==1 and len(permissionSplit)==2 and curNode.tag == assetTemp:
														asset += "*"
														continue
													if ("application" not in curNode.tag) and ("configuration" not in curNode.tag) and (curNode.tag in assetTemp):
														assetTemp = assetTemp.replace(curNode.tag,"")
														asset += assetTemp
														continue
													else:
														asset += assetTemp#
													if i == (len(permissionSplit)-1):#
														break#
													else:#
														asset+="_"#
											if self.tree.contains(title+"."+methodName) is True:#
												continue
											self.tree.create_node(asset,title+"."+methodName,parent=curNode.identifier,data=verb)#
											m.pdist[str(permission)]+=1
											interfaceParam = interfaceLocation + "."+methodName
											f1.write("Action word = " + verb + "\n" + "Resource words = \n" + "Resource correlation = \n")
									#self.insertDB('floodlight',interfaceParam,'None',description,permission)
								#code = allTd[0].findAll('code')[0]
								#if len(code.findAll('a')) is not 0:
								#	code.findAll('a')[0].contents[0]
								#else:
								#	print "<"+allTd[0].findAll('code')[0].contents[0]+">"
						print '\n'
		self.tree.show()
		print '------------------------------------------------------------------------------------------------------------'
		f1.write(str(self.tree.show()))
		f1.close()

	#def refineTree(self)
		#rootNode = self.tree.get_node("ROOT")#
		#rootSibilings = rootNode.fpointer
		#for sibling in rootSiblings:
		#	siblingNode = tree.get_node(sibling)
		#	dsiblingNode = siblingNode.fpointer
		#		for dsibling in siblintNode
	def getFloodlightDesc(self):
		methodNum=1
		rootNode = self.tree.get_node("ROOT")#
		curNode = rootNode#
		url='http://floodlight.github.io/floodlight/javadoc/floodlight/net/floodlightcontroller/core/module/'
		mainPage = 'IFloodlightService.html'
		mainDoc = urllib2.urlopen(url+mainPage)
		soup = bs(mainDoc.read(),"lxml")
		allInterfaces = soup.find('div', {"class": "description"})
		interfaceList = allInterfaces.findAll('a')
		f1 = open('/home/sdn/floodlight.text', 'w+')
		printNum = 1
		for interface in interfaceList:
			interfaceLink = interface.get('href')
			if interfaceLink == "../../../../net/floodlightcontroller/storage/AbstractStorageSource.html":
				print "adsf"
				break
			interfaceDoc = urllib2.urlopen(url+interfaceLink)
			soup2 = bs(interfaceDoc.read(),"lxml")
			subTitle = soup2.find('div', {"class":"subTitle"}).contents[0]
			title = soup2.find('h2',{"class":"title"}).contents[0].split('Interface ')[1]
			if soup2.find('h2',{"class":"title"}).contents[0].startswith('Class') is True:
				continue
			else:
				title = soup2.find('h2',{"class":"title"}).contents[0].split('Interface ')[1]
				print "Title : " + title
				interfaceLocation = subTitle+"."+title
				f1.write("["+str(printNum)+"] API Package path : "+interfaceLocation+"\n")
				printNum+=1
				print "API: "+interfaceLocation

				assetsString = subTitle.split("net.floodlightcontroller.")#
				print len(assetsString)
				if len(assetsString) is 1:
					continue
				assets = assetsString[1].split(".")#
				for i in range(len(assets)):#
					if self.tree.contains(assets[i]) is True:#
						curNode = self.tree.get_node(assets[i])
						continue#
					else:#
						curNode = self.tree.create_node(assets[i],assets[i],parent=curNode.identifier)#
				interfaceLocation = subTitle+"."+title
			
			#implementationDiv = soup2.find('div', {"class": "description"}) #Implementation
			#implementations = implementationDiv.findAll('dl')
				f1.write("["+str(printNum)+"] API Package path : "+interfaceLocation+"\n")
				printNum+=1
				print "API: "+interfaceLocation

				methodSummary = soup2.find('table',{"summary":"Method Summary table, listing methods, and an explanation"})
				if methodSummary is not None:
					allTr = methodSummary.findAll('tr')
					if len(allTr) is not 0:
						for tr in allTr:
							allTd = tr.findAll("td")
							if len(allTd) is not 0:
								methodName = allTd[1].find('span',{"class":"memberNameLink"}).contents[0].contents[0]
								descriptionDiv = allTd[1].find('div',{"class":"block"})
								if descriptionDiv is not None:
									deprecatedLabel = descriptionDiv.find('span',{"class":"deprecatedLabel"})
									if deprecatedLabel is not None:
										description=deprecatedLabel.contents[0]
									else:
										description = descriptionDiv.contents[0]
									#description = description.rstrip('\n')
									if description.count(':') is not 0:
										description = description.split(":")
										description = description[0]
									Ndesc = ''
									descTemp = description.split()
									for i in range(0,len(descTemp)):
										
										Ndesc += descTemp[i] + " "
									print Ndesc
									f1.write("{"+str(methodNum)+"}"+"- ["+methodName+"]"+"\n")
									methodNum=methodNum+1
									f1.write("Description : " + Ndesc + "\n")
									permission = self.semanticParsing2(Ndesc,f1,0)
									permissionSplit = permission.split("_")#
									if len(permissionSplit) is 1:
										permission = self.semanticParsing2(Ndesc,f1,1)
										permissionSplit = permission.split("_")#
									verb = permissionSplit[0]#
									asset = ""#
									if len(permissionSplit) is 1:
										asset = "None"
									else:
										for i in range (1,len(permissionSplit)):#
											assetTemp = permissionSplit[i]#
											if ("set" in assetTemp):
												assetTemp = assetTemp.replace("set","")
											if ("list" in assetTemp) and (assetTemp is not "listener"):
												assetTemp = assetTemp.replace("list","")
											if "collection" in assetTemp:
												assetTemp = assetTemp.replace("collection","")
											if "infrastructure" in assetTemp:
												assetTemp = assetTemp.replace("infrastructure","")
											if "factory" in assetTemp:
												assetTemp = assetTemp.replace("factory","")
											if "summary" in assetTemp:
												assetTemp = assetTemp.replace("summary","")
											if "total" in assetTemp:
												assetTemp = assetTemp.replace("total","")
											if assetTemp == "":
												assets=asset.rstrip('_')
												continue
											if i==1 and len(permissionSplit)==2 and curNode.tag == assetTemp:
												asset += "*"
												continue
											if ("application" not in curNode.tag) and ("configuration" not in curNode.tag) and (curNode.tag in assetTemp):
												assetTemp = assetTemp.replace(curNode.tag,"")
												asset += assetTemp
												continue
											else:
												asset += assetTemp#
											if i == (len(permissionSplit)-1):#
												break#
											else:#
												asset+="_"#
									if self.tree.contains(title+"."+methodName) is True:#
										continue
									self.tree.create_node(asset,title+"."+methodName,parent=curNode.identifier,data=verb)#
									m.pdist[str(permission)]+=1
									interfaceParam = interfaceLocation + "."+methodName
									f1.write("Action word = " + verb + "\n" + "Resource words = \n" + "Resource correlation = \n")
									#self.insertDB('floodlight',interfaceParam,'None',description,permission)
								#code = allTd[0].findAll('code')[0]
								#if len(code.findAll('a')) is not 0:
								#	code.findAll('a')[0].contents[0]
								#else:
								#	print "<"+allTd[0].findAll('code')[0].contents[0]+">"
				print '\n'
		self.tree.show()
		print '------------------------------------------------------------------------------------------------------------'
		f1.write(str(self.tree.show()))
		f1.close()
			if len(implementations) is 1:
				dt = implementations[0].findAll('dt')
				dtValue=dt[0].contents[0]
				if dtValue is "All Implemented Interfaces:" :
			if len(implementations) is not 1:
				if len(implementations) is 2:
					implementationLinks = implementations[1].findAll('a')
					for implementationLink in implementationLinks:
						implLink = implementationLink.get('href')
						implDoc = urllib2.urlopen(url+interfaceLink+"/../"+implLink)
						soup3 = bs(implDoc.read(),"lxml")
						implSubTitle = soup3.find('div', {"class":"subTitle"}).contents[0]
						implTitle = soup3.find('h2',{"class":"title"}).contents[0].split('Class ')[1]
						print "Impl: "+ implSubTitle+"."+implTitle
					
				if len(implementations) is 3:
					implementationLinks = implementations[2].findAll('a')
					for implementationLink in implementationLinks:
						implLink = implementationLink.get('href')
						implDoc = urllib2.urlopen(url+interfaceLink+"/../"+implLink)
						soup3 = bs(implDoc.read(),"lxml")
						implSubTitle = soup3.find('div', {"class":"subTitle"}).contents[0]
						implTitle = soup3.find('h2',{"class":"title"}).contents[0].split('Class ')[1]
						print "Impl: "+ implSubTitle+"."+implTitle

	def preprocessing(self,desc):
		#desc = re.sub("\[\]\{\}\!\@\#\$\%\^\&\*\(\)\?'\:\;,+","",desc)
		#desc = desc.translate(",!@#$%^&*()':{}[]`")
		desc = desc.replace(","," ")
		desc = desc.replace("!","")
		desc = desc.replace("@","")
		desc = desc.replace("#","")
		desc = desc.replace("%","")
		desc = desc.replace("(","")
		desc = desc.replace(")","")
		desc = desc.replace(":","")
		desc = desc.replace("{","")
		desc = desc.replace("}","")
		desc = desc.replace("`","")
		desc = desc.replace("[","")
		desc = desc.replace("]","")
		desc = desc.replace("'","")
		desc = desc.replace("*","")
		desc = desc.replace("&","")
		desc = desc.replace("^","")
		print desc
		if "I/O" in desc:
			desc = desc.replace("I/O","IO")
		desc = desc.replace("/"," and ")
		tokenized = nltk.word_tokenize(desc)
		posTag = nltk.pos_tag(tokenized)
		grammar = '''
		RB: {<RB> | <RBS> | <RBR>}'''
		chunker = RegexpParser(grammar)
		chunked = chunker.parse(posTag)
		print chunked

		for n in range(len(chunked)):
			if str(chunked[n]).startswith('(RB') is True:
				if n is 0 :
					s = str(chunked[n]).split(" ")
					ss = s[1].split("/")
					removalWord = ss[0]
					desc = desc.replace(removalWord+" ","")
				if n>0 and n<=len :
					s = str(chunked[n]).split(" ")
					ss = s[1].split("/")
					removalWord = ss[0]
					desc = desc.replace(" "+removalWord,"")
		print desc
		return desc

	def semanticParsing2(self,desc,f1,flag):
		tempTokenized=nltk.word_tokenize(desc)
		tempTagged = nltk.pos_tag(tempTokenized)
		print tempTagged
		if desc is "Deprecated.":
			return None
		url='http://cogcomp.cs.illinois.edu/demo_files/SRL.php'
		desc = self.preprocessing(desc)
		splitDesc = desc.split(' ')
		ttverb=desc.split(' ')[0]
		if flag is 0:
			tempdesc = "Write"
		else:
			tempdesc = ttverb
		for i in range(1,len(splitDesc)):
			tempdesc += " "+splitDesc[i]
		tempdesc = tempdesc.lower()
		data = "{\"text\":\"It "+tempdesc+"\"}"
		print data#

		r = requests.post(url,data)
		soup = bs(r.text,"lxml")

		verticalDiv = soup.find('table', {"id":"verticaltable"})

		allTrs = verticalDiv.findAll('tr')

		allTds = allTrs[0].findAll('td')
		sentence = [0 for _ in range(len(allTds))]
		index = 0
		for td in allTds:
			if index is 0:
				index+=1
				continue
			else:
				sentence[index] = td.contents[0]
				index += 1
		srlTds = allTrs[1].findAll('td')
		objectStartingPoint = 0
		objectEndPoint =0
		for tdtd in srlTds:#search object starting point
			try:
				if "A1" in str(tdtd["class"]):
					objectEndPoint = int(tdtd["colspan"])
					#print objectEndPoint
					break;
				size = int(tdtd["colspan"])
			except (ValueError, KeyError) as e:
				size = 1
			objectStartingPoint += size

		objectStartingPoint+=1
		objectEndPoint += objectStartingPoint
		#print objectStartingPoint
		#print objectEndPoint
		objectSentence="" #for test && use
		for num in range(objectStartingPoint,objectEndPoint):
			objectSentence += str(sentence[num])			

		#print objectSentence
		#nltk.download('punkt')
		#nltk.download('maxent_treebank_pos_tagger')
		text=nltk.word_tokenize(objectSentence)
		tagged = nltk.pos_tag(text)
		#print tagged
		grammar = '''
		NP: {<NN>.*<NNS>.* | <NNS>.*<NN>.* | <NN>.*<NN>.* | <NN> | <NNS>}
		TO: {<TO.*>}
		VBG: {<VBG>.*}
		WDT: {<WDT.*>}
		WRB: {<WRB>.*}
		IN: {<IN.*>}'''
		chunker = RegexpParser(grammar)
		chunked = chunker.parse(tagged)
		chunkedSentence=str(chunked) #for test print
		parsingRe = re.compile(" \w+/")
		permission = ''
		ttverb = inflection.singularize(ttverb)
		sy1 = wordnet.synsets(ttverb)
		sy2 = wordnet.synsets('read')
		sy3 = wordnet.synsets('write')
		#for s in sy1:
		#	for t in sy2:
		#		if wordnet.path_similarity(s,t) > 0.3:
		#			ttverb = 'Read'
		#for s in sy1:
		#	for t in sy3:
		#		if wordnet.path_similarity(s,t) >0.3:
		#			ttverb = 'Write'
		
		ttverb = self.onosVerbConverter(ttverb)
		permission += ttverb
		m.vdist[ttverb] +=1
		preOf = ''
		afterOf = ''
		ofFlag = 0
		for n in range(len(chunked)):
			if str(chunked[0]).startswith('(IN') is True:
				permission = ttverb+self.supportInTo(objectSentence,"IN")
				print permission
				return permission
			if str(chunked[0]).startswith('(TO') is True:
				split1 = str(chunked[1]).split("('")
				split2 = str(split1[1]).split("'")
				ttverb=split2[0]
				ttverb = self.onosVerbConverter(ttverb)
				permission = ttverb+self.supportInTo(objectSentence,"TO")
				print permission
				return permission
			if str(chunked[n]).startswith('(NP') is True:
				s = parsingRe.findall(str(chunked[n]))
				for ss in s:
					if ofFlag is 0:
						temp = ss.translate(None, " /")
						singular = inflection.singularize(temp)
						m.ndist[singular]+=1
						preOf +="_"+singular
						
					else:
						temp = ss.translate(None, " /")
						singular = inflection.singularize(temp)
						m.ndist[singular]+=1
						afterOf +="_"+singular
						
			if (str(chunked[n]).startswith('(TO') | str(chunked[n]).startswith('(WDT') | str(chunked[n]).startswith('(WRB')) is True: # str(chunked[n]).startswith('(VBG') | 	
				permission = permission+afterOf+preOf
				print permission		
				return permission
			if str(chunked[n]).startswith('(VBG'):
				s = parsingRe.findall(str(chunked[n]))
				for ss in s:
					if ss == "using":
						permission = permission+afterOf+preOf
						print permission
						return permission
			if str(chunked[n]).startswith('(IN') is True:
				s = parsingRe.findall(str(chunked[n]))
				for ss in s:
					if ss.translate(None, " /") == "of" or (ss.translate(None, " /") == "about"):#FIX ME!!!!!!!!!!!!!!!!!!!!!!
						ofFlag=1
						break
					else:	
						permission = permission+afterOf+preOf
						print permission
						return permission
		permission = permission+afterOf+preOf	
		print permission
		return permission

		#tt= map(lambda x: list(x.subtress(filter=lambda x: x.node=='NN')),NPs)

	def supportFullsentence(self,desc):
		text=nltk.word_tokenize(objectSentence)
		tagged = nltk.pos_tag(text)
		print tag


	def onosVerbConverter(self,verb):
		for readVerb in m.onos_read_verb:
			if readVerb == verb:
				verb="Read"
				break
		for writeVerb in m.onos_write_verb:
			if writeVerb == verb:
				verb="Write"
				break
		for executeVerb in m.onos_execute_verb:
			if executeVerb == verb:
				verb="Execute"
				break
		return verb
	def semanticParsing(self,desc):
		url='http://cogcomp.cs.illinois.edu/demo_files/SRL.php'
	
		ddesc = desc.lower()
		
		data = "{\"text\":\"It "+ddesc+"\"}"

		r = requests.post(url,data)
		soup = bs(r.text,"lxml")
		table = soup.find('table', {"class":"table table-condensed"})

		f1 = open('/home/sdn/temp.txt', 'w+')
		rows = table.findAll('tr')

		pattern = re.compile("col_..nom",re.IGNORECASE)

#for row in rows:
		permission = ''
#verb = table.findAll('td',{'class':'col_2 V'})
#tverb = verb[0].findAll('a', href=True)
#ttverb = tverb[0]
		ttverb=desc.split(' ')[0]
		permission+=ttverb
		
		cols = table.findAll('td',{'class':pattern})
		for column in cols:
			a = column.findAll('a', href=True)
			if a is not None:
				for atemp in a:
					tt = str(atemp)
					if tt.find("NOM") != -1:
						permission+='_'+atemp.contents[0].split('.')[0]
				#print "Resource : " + atemp.contents[0].split('.')[0]
					else:
						print 'not a NOM'
		return permission

	def supportInTo(self,desc,meaning):
		if desc is "Deprecated.":
			return None
		url='http://cogcomp.cs.illinois.edu/demo_files/SRL.php'

		data = "{\"text\":\""+desc+"\"}"
		print data#

		r = requests.post(url,data)
		soup = bs(r.text,"lxml")

		verticalDiv = soup.find('table', {"id":"verticaltable"})

		allTrs = verticalDiv.findAll('tr')

		allTds = allTrs[0].findAll('td')
		sentence = [0 for _ in range(len(allTds))]
		index = 0
		for td in allTds:
			if index is 0:
				index+=1
				continue
			else:
				sentence[index] = td.contents[0]
				index += 1
		srlTds = allTrs[1].findAll('td')
		objectStartingPoint = 0
		objectEndPoint =0
		print objectStartingPoint + objectEndPoint
		for tdtd in srlTds:#search object starting point
			try:
				if "A1" in str(tdtd["class"]):
					objectEndPoint = int(tdtd["colspan"])
					#print objectEndPoint
					break;
				size = int(tdtd["colspan"])
			except (ValueError, KeyError) as e:
				size = 1
			objectStartingPoint += size

		objectStartingPoint+=1
		objectEndPoint += objectStartingPoint
		#print objectStartingPoint
		#print objectEndPoint
		objectSentence="" #for test && use
		for num in range(objectStartingPoint,objectEndPoint):
			objectSentence += str(sentence[num])			

		print objectSentence
		
		text=nltk.word_tokenize(objectSentence)
		tagged = nltk.pos_tag(text)
		#print tagged

		grammar = '''
		NP: {<NN>.*<NNS>.* | <NNS>.*<NN>.* | <NN>.*<NN>.* | <NN> | <NNS>}
		TO: {<TO.*>}
		WDT: {<WDT.*>}
		IN: {<IN.*>}'''
		chunker = RegexpParser(grammar)
		chunked = chunker.parse(tagged)
		chunkedSentence=str(chunked) #for test print
		parsingRe = re.compile(" \w+/")
		permission = ''
		sy2 = wordnet.synsets('read')
		sy3 = wordnet.synsets('write')

		
		permission = ''
		preOf = ''
		afterOf = ''
		ofFlag = 0
		for n in range(len(chunked)):
			if str(chunked[n]).startswith('(NP') is True:
				s = parsingRe.findall(str(chunked[n]))
				for ss in s:
					if ofFlag is 0:
						temp = ss.translate(None, " /")
						singular = inflection.singularize(temp)
						preOf +="_"+singular
						#print preOf
					else:
						temp = ss.translate(None, " /")
						singular = inflection.singularize(temp)
						afterOf +="_"+singular
						#print afterOf
			if (str(chunked[n]).startswith('(TO') | str(chunked[n]).startswith('(WDT')) is True: # str(chunked[n]).startswith('(VBG') | 	
				permission = permission+afterOf+preOf	
				print permission		
				return permission
			if str(chunked[n]).startswith('(IN') is True:
				s = parsingRe.findall(str(chunked[n]))
				for ss in s:
					if (ss.translate(None, " /") == "of") or (ss.translate(None, " /") == "about"):#FIX ME!!!!!!!!!!!!!!!!!!!!!!
						ofFlag=1
						break
					else:	
						permission = permission+afterOf+preOf
						print permission
						return permission
		permission = permission+afterOf+preOf	
		print permission
		return permission



	def __init__(self):
		vdist = FreqDist()
		ndist = FreqDist()
		print 'init'

m = makePermission()

m.getOnosDesc()
