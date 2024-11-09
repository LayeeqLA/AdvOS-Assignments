import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

data = pd.read_csv('results.csv')
# print(data)

# c,Protocol,Message Complexity,Response Time, System Throughput,Record Final
sns.lineplot(x="c", y="Message Complexity", hue="Protocol", data=data)
plt.xlabel("Mean CS Execution Time (c) [ms]")
plt.ylabel("Message Complexity")
plt.title("Mean Message Complexity")
plt.savefig('amc.png')
plt.clf()

sns.lineplot(x="c", y="Response Time", hue="Protocol", data=data)
plt.xlabel("Mean CS Execution Time (c) [ms]")
plt.ylabel("Response Time [ms]")
plt.title("Mean Response Time")
plt.savefig('art.png')
plt.clf()

sns.lineplot(x="c", y="System Throughput", hue="Protocol", data=data)
plt.xlabel("Mean CS Execution Time (c) [ms]")
plt.ylabel("System Throughput")
plt.title("Mean System Throughput")
plt.savefig('ast.png')
plt.clf()