# generate the documentation as a pdf

Tested in Ubuntu:

```
sudo apt-get install pandoc texlive-latex-base texlive-fonts-recommended texlive-extra-utils texlive-latex-extra
pandoc  contribute.md architecture.md configuration-system.md  storage.md technical-notes/* extends/* deploy.md operate.md start-dev.md  -V geometry:margin=1in  -o iris-dp-devdoc.pdf
```