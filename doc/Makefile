stackframe.tex:	stackframe.pic
	pic -t stackframe.pic > stackframe.tex

PAL.pdf:	PAL.tex stackframe.tex
	latex PAL.tex
	latex PAL.tex
	latex PAL.tex
	dvips PAL.dvi -Ppdf -o PAL.ps
	ps2pdf PAL.ps

clean:
	rm -f PAL.aux PAL.dvi PAL.lof PAL.log PAL.lot PAL.pdf PAL.ps PAL.toc
	rm -f stackframe.tex
