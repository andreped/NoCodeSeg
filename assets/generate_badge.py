import numpy as np
import pandas as pd
import aspose.words as aw


path = "filedownloads?parentAlias=ntnu"
data = np.asarray(pd.read_csv(path, delimiter=None))
pid2find = "10.18710/TLA01U"

res = []
total = 0
for i in range(data.shape[0]):
	id_, pid_, count_ = data[i]
	if pid2find in pid_:
		total += count_

ret = '<a href="https://doi.org/' + \
          pid2find + \
          '"><img src="https://img.shields.io/badge/DataverseNO%20downloads-' + \
          str(total) + \
          '-orange"></a>'

print("generated html line: ", ret)

# with open("test.html", "w") as f:
# 	f.write(ret)

# create blank document
doc = aw.Document()

# Use a document builder to add content to the document
builder = aw.DocumentBuilder(doc)

# Write a new paragraph in the document with the text generated above into the file
builder.writeln(ret)

# Save the document in DOCX format. Save format is automatically determined from the file extension.
doc.save("test.svg")
