import mkdocs_gen_files
import os
from distutils.dir_util import copy_tree

# Move README to index unless there's already an index.md 
if(os.path.exists("/docs/docs/index.md") != True): 
  readme = open("/docs/README.md").read()
  with mkdocs_gen_files.open("index.md", "w") as f: 
    print(readme, file=f) 

# Move CONTRIBUTING to developing
if(os.path.exists("/docs/CONTRIBUTING.md")):
  readme = open("/docs/CONTRIBUTING.md").read()
  with mkdocs_gen_files.open("contributing.md", "w") as f: 
    print(readme, file=f)

# Move Library Docs
rootdir = "/docs/libraries"
for library in os.listdir(rootdir):
    file = os.path.join(rootdir, library, "README.md")
    if(os.path.exists(file)): 
      readme = open(file).read()
      with mkdocs_gen_files.open(f"libraries/{library}.md", "w") as f:
        print(readme, file=f)
    resources = os.path.join(rootdir, library, "resources")
    if(os.path.exists(resources)):
      copy_tree(resources, "/libraries/resources")