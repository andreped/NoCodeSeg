import numpy as np
import pandas as pd
import imgkit


options = {
    'format': 'png',
    'crop-h': '30',
    'crop-w': '70',
    'crop-x': '5',
    'crop-y': '5',
    'encoding': "UTF-8",
    'custom-header' : [
        ('Accept-Encoding', 'gzip')
    ]
}


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

imgkit.from_string(ret, "test.png", options=options)
