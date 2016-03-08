package jgi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

import stream.Read;
import stream.SamLine;
import stream.ScaffoldCoordinates;
import stream.SiteScore;

import align2.ReadStats;
import align2.Tools;

import dna.ChromosomeArray;
import dna.CoverageArray;
import dna.CoverageArray2;
import dna.CoverageArray3;
import dna.Data;
import dna.Gene;
import dna.Parser;
import dna.Scaffold;
import dna.Timer;
import fileIO.ReadWrite;
import fileIO.TextFile;
import fileIO.TextStreamWriter;

/**
 * @author Brian Bushnell
 * @date Jan 4, 2013
 *
 */
public class CoveragePileup {
	
	/*--------------------------------------------------------------*/
	/*----------------             Main             ----------------*/
	/*--------------------------------------------------------------*/
	
	public static void main(String[] args){
		CoveragePileup sp=new CoveragePileup(args);
		
		Timer t=new Timer();
		t.start();
		
		sp.process();
		
		t.stop();
		Data.sysout.println();
		Data.sysout.println("Time: \t"+t);
		
	}
	
	/*--------------------------------------------------------------*/
	/*----------------        Initialization        ----------------*/
	/*--------------------------------------------------------------*/
	
	public CoveragePileup(String[] args){
		for(String s : args){
			if(s.contains("=stdout")){Data.sysout=System.err;}
		}
		System.err.println("Executing "+(this.getClass().getName())+" "+Arrays.toString(args)+"\n");
		
		boolean bs=false, setbs=false;
		boolean outset=false;
		ReadWrite.USE_UNPIGZ=true;
		
		for(int i=0; i<args.length; i++){
			final String arg=args[i];
			final String[] split=arg.split("=");
			String a=split[0].toLowerCase();
			String b=split.length>1 ? split[1] : null;
			if("null".equalsIgnoreCase(b)){b=null;}
//			System.err.println("Processing "+args[i]);
			
			if(Parser.isJavaFlag(arg)){
				//jvm argument; do nothing
			}else if(Parser.parseZip(arg, a, b)){
				//do nothing
			}else if(a.equals("ref") || a.equals("reference") || a.equals("fasta")){
				reference=b;
			}else if(a.equals("in") || a.equals("in1")){
				in=b;
			}else if(a.equals("out") || a.equals("coveragestats") || a.equals("covstats") || a.equals("stats")){
				covstats=b;
				outset=true;
			}else if(a.equals("minscaf") || a.equals("covminscaf")){
				minscaf=Integer.parseInt(b);
			}else if(a.equals("outsam")){
				outsam=b;
			}else if(a.equals("outorf")){
				outorf=b;
			}else if(a.equals("orffasta") || a.equals("fastaorf")){
				orffasta=b;
			}else if(a.equals("basecov") || a.equals("outcov")){
				basecov=b;
			}else if(a.equals("bincov") || a.equals("outbinned")){
				bincov=b;
			}else if(a.equals("normcov") || a.equals("outnormalized")){
				normcov=b;
			}else if(a.equals("normcovo") || a.equals("outnormalizedoverall")){
				normcovOverall=b;
			}else if(a.equals("delta")){
				DELTA_ONLY=Tools.parseBoolean(b);
			}else if(a.equals("hist") || a.equals("histogram") || a.equals("covhist")){
				histogram=b;
			}else if(a.equals("reads")){
				maxReads=Tools.parseKMG(b);
			}else if(a.equals("scafs") || a.equals("scaffolds")){
				initialScaffolds=Tools.max(128, (int)(Tools.min(Long.parseLong(b),2000000000)));
			}else if(a.equals("binsize")){
				binsize=Integer.parseInt(b);
			}else if(a.equals("32bit")){
				bits32=Tools.parseBoolean(b);
			}else if(a.equals("bitset") || a.equals("usebitset")){
				bs=Tools.parseBoolean(b);
				setbs=true;
			}else if(a.equals("arrays") || a.equals("usearrays") || a.equals("median") || a.equals("calcmedian")){
				bs=!Tools.parseBoolean(b);
				setbs=true;
			}else if(a.startsWith("nonzero") || a.equals("nzo")){
				NONZERO_ONLY=Tools.parseBoolean(b);
				System.err.println("Set NONZERO_ONLY to "+NONZERO_ONLY);
			}else if(a.equals("append") || a.equals("app")){
				append=ReadStats.append=Tools.parseBoolean(b);
			}else if(a.equals("overwrite") || a.equals("ow")){
				overwrite=Tools.parseBoolean(b);
				System.err.println("Set overwrite to "+overwrite);
			}else if(a.equalsIgnoreCase("twocolumn")){
				TWOCOLUMN=Tools.parseBoolean(b);
				System.err.println("Set TWOCOLUMN to "+TWOCOLUMN);
			}else if(a.equalsIgnoreCase("countgc")){
				COUNT_GC=Tools.parseBoolean(b);
				System.err.println("Set COUNT_GC to "+COUNT_GC);
			}else if(a.equals("secondary") || a.equals("usesecondary")){
				USE_SECONDARY=Tools.parseBoolean(b);
				System.err.println("Set USE_SECONDARY_ALIGNMENTS to "+USE_SECONDARY);
			}else if(a.equals("keepshortbins") || a.equals("ksb")){
				KEEP_SHORT_BINS=Tools.parseBoolean(b);
				System.err.println("Set KEEP_SHORT_BINS to "+KEEP_SHORT_BINS);
			}else if(a.equals("strandedcoverage") || a.equals("strandedcov") || a.equals("covstranded") || a.equals("stranded")){
				STRANDED=Tools.parseBoolean(b);
			}else if(a.equals("startcov") || a.equals("covstart") || a.equals("startonly")){
				START_ONLY=Tools.parseBoolean(b);
			}else if(a.equals("concise")){
				CONCISE=Tools.parseBoolean(b);
			}else if(a.equals("verbose")){
				verbose=Tools.parseBoolean(b);
			}else if(a.equals("normc") || a.equals("normalizecoverage")){
				NORMALIZE_COVERAGE=Tools.parseBoolean(b);
			}else if(a.equals("header") || a.equals("hdr")){
				printHeader=Tools.parseBoolean(b);
			}else if(a.equals("headerpound") || a.equals("#")){
				headerPound=Tools.parseBoolean(b);
			}else if(a.equals("normb") || a.equals("normalizebins")){
				try {
					NORMALIZE_LENGTH_BINS=Integer.parseInt(b);
				} catch (NumberFormatException e) {
					boolean x=Tools.parseBoolean(b);
					NORMALIZE_LENGTH_BINS=x ? 100 : -1;
				}
			}else{
				throw new RuntimeException("Unknown parameter: "+args[i]);
			}
			
		}
		
		if(setbs){
			USE_BITSETS=bs;
			USE_COVERAGE_ARRAYS=!bs;
			System.err.println("Set USE_BITSETS to "+USE_BITSETS);
		}else{
			if(histogram==null && basecov==null && bincov==null && normcov==null && normcovOverall==null && outorf==null){//No need for coverage array!
				USE_COVERAGE_ARRAYS=false;
				if(TWOCOLUMN){//No need for bitset, either!
					USE_BITSETS=false;
				}else{
					USE_BITSETS=true;
				}
				System.err.println("Set USE_COVERAGE_ARRAYS to "+USE_COVERAGE_ARRAYS);
				System.err.println("Set USE_BITSETS to "+USE_BITSETS);
			}
		}
//		assert(false) : USE_COVERAGE_ARRAYS;
		
		if(maxReads<0){maxReads=Long.MAX_VALUE;}
		{
			final String a=(args.length>0 ? args[0] : null);
			final String b=(args.length>1 ? args[1] : null);
			if(in==null && a!=null && a.indexOf('=')<0 && (a.startsWith("stdin") || new File(a).exists())){in=a;}
			if(covstats==null && b!=null && b.indexOf('=')<0){covstats=b;}
			if(in==null){in="stdin";}
			if(covstats==null && !outset){
//				out="stdout";
//				System.err.println("Warning: output destination not set; producing no output.  To print to standard out, set 'out=stdout'");
				Data.sysout=System.err;
			}
		}
		assert(in!=null);
//		assert(out!=null || outset) : "Output file was not set.";
		
		if(STRANDED){
			assert(basecov==null || basecov.indexOf('#')>=0) : "Output filenames must contain '#' symbol for strand-specific output.";
			assert(bincov==null || bincov.indexOf('#')>=0) : "Output filenames must contain '#' symbol for strand-specific output.";
			assert(normcov==null || normcov.indexOf('#')>=0) : "Output filenames must contain '#' symbol for strand-specific output.";
			assert(normcovOverall==null || normcovOverall.indexOf('#')>=0) : "Output filenames must contain '#' symbol for strand-specific output.";
			assert(histogram==null || histogram.indexOf('#')>=0) : "Output filenames must contain '#' symbol for strand-specific output.";
			assert(covstats==null || covstats.indexOf('#')>=0) : "Output filenames must contain '#' symbol for strand-specific output.";
		}
		
		if(!Tools.testOutputFiles(overwrite, append, false, basecov, bincov, normcov, normcovOverall, histogram, covstats)){
			throw new RuntimeException("\n\noverwrite="+overwrite+"; Can't write to output files "+
					basecov+", "+bincov+", "+normcov+", "+normcovOverall+", "+histogram+", "+covstats+"\n");
		}
	}

	
	/*--------------------------------------------------------------*/
	/*----------------       Data Structures        ----------------*/
	/*--------------------------------------------------------------*/
	
	
	/** The goal if this is to garbage-collect unnecessary objects, not really for reusing the object */
	public void clear(){
		list=null;
		table=null;
		
		program=null;
		version=null;
		
		in=null;
		covstats=null;
		outsam=null;
		outorf=null;
		reference=null;
		histogram=null;
		basecov=null;
		bincov=null;
		normcov=null;
		normcovOverall=null;
		orffasta=null;
		
		error=false;

		refBases=0;
		mappedBases=0;
		mappedReads=0;
		readsProcessed=0;
		totalCoveredBases1=0;
		totalCoveredBases2=0;
		scaffoldsWithCoverage1=0;
		scaffoldsWithCoverage2=0;
		totalScaffolds=0;
	}
	
	public void createDataStructures(){
		refBases=0;
		mappedBases=0;
		mappedReads=0;
		readsProcessed=0;
		totalCoveredBases1=0;
		totalCoveredBases2=0;
		scaffoldsWithCoverage1=0;
		scaffoldsWithCoverage2=0;
		totalScaffolds=0;
		error=false;
		list=new ArrayList<Scaffold>(initialScaffolds);
		table=new HashMap<String, Scaffold>(initialScaffolds);
	}
	
	
	/*--------------------------------------------------------------*/
	/*----------------         Outer Methods        ----------------*/
	/*--------------------------------------------------------------*/
	
	/** Read and process all input data. */
	public void process(){
		createDataStructures();
		
		TextFile tf=new TextFile(in, ReadWrite.USE_UNPIGZ, false);
		
		final TextStreamWriter tsw=(outsam==null ? null : new TextStreamWriter(outsam, overwrite, false, true));
		if(outsam!=null){tsw.start();}
		
		String line=processHeader(tf, tsw);
		
		processReference();
		
		if(maxReads<0){maxReads=Long.MAX_VALUE;}
		for(; line!=null && readsProcessed<maxReads; line=tf.nextLine()){
			if(tsw!=null){tsw.println(line);}
			processSamLine(line);
		}
		
		tf.close();
		if(tsw!=null){tsw.poison();}
		
		printOutput();
		
		if(orffasta!=null){
			processOrfsFasta(orffasta, outorf, table);
		}
		
		if(tsw!=null){tsw.waitForFinish();}
	}
	
	
	/*--------------------------------------------------------------*/
	/*----------------             Setup            ----------------*/
	/*--------------------------------------------------------------*/
	
	
	/** Process all sam header lines from the tf.
	 * Once a non-header line is encountered, return it. 
	 * If non-null, print all lines to the tsw. */ 
	public String processHeader(TextFile tf, TextStreamWriter tsw){
		String line=null;
		for(line=tf.nextLine(); line!=null && line.startsWith("@"); line=tf.nextLine()){
			if(tsw!=null){tsw.println(line);}
			
			final String[] split=line.split("\t");
			final String a=split[0];
			
			if(a.equals("@SQ")){
				Scaffold scaf=new Scaffold(split);
				if(COUNT_GC){scaf.basecount=new long[8];}
				assert(!table.containsKey(scaf.name)) : "\nDuplicate scaffold name!\n"+scaf+"\n\n"+table.get(scaf.name);
				table.put(scaf.name, scaf);
				list.add(scaf);
				refBases+=scaf.length;
//				sc.obj=new CoverageArray2(table.size(), sc.length+1);
//				Data.sysout.println("Made scaffold "+sc.name+" of length "+sc.length);
			}else if(a.equals("@PG")){
				for(String s : split){
					if(s.startsWith("PN:")){
						if(program==null){program=Data.forceIntern(s.substring(3));}
					}else if(s.startsWith("VN:")){
						if(version==null){version=Data.forceIntern(s.substring(3));}
					}
				}
			}else if(a.equals("@RG")){
				//Do nothing
			}else if(a.equals("@HD")){
				//Do nothing
			}else if(a.equals("@CO")){
				//Do nothing
			}else{
//				assert(false) : line;
			}
		}
		return line;
	}
	
	
	public void loadScaffoldsFromIndex(int minChrom, int maxChrom){
		
		final int[][] lengths=Data.scaffoldLengths;
		final int[][] locs=Data.scaffoldLocs;
		final byte[][][] names=Data.scaffoldNames;
		final int[] counts=new int[8];
		for(int chrom=minChrom; chrom<=maxChrom; chrom++){
			final ChromosomeArray ca=Data.getChromosome(chrom);
//			assert(false) : lengths[chrom]+", "+lengths.length+", "+names[chrom]+", "+locs[chrom]+", "+(ca==null);
			if(lengths[chrom]!=null){
				final int[] clengths=lengths[chrom];
				final int[] clocs=locs[chrom];
				final byte[][] cnames=names[chrom];
				for(int idx=0; idx<clengths.length; idx++){
					final int length=clengths[idx];
					final int loc=clocs[idx];
					final String name=new String(cnames[idx]);
					final Scaffold scaf=new Scaffold(name, length);
					if(ca!=null){
						scaf.gc=ca.calcGC(loc, length, counts);
					}
					if(COUNT_GC){scaf.basecount=new long[8];}
					assert(!table.containsKey(scaf.name)) : "\nDuplicate scaffold name!\n"+scaf+"\n\n"+table.get(scaf.name);
					table.put(scaf.name, scaf);
					list.add(scaf);
					refBases+=scaf.length;
				}
			}
		}
	}
	
	
	public void processReference(){
		if(reference==null){return;}

		TextFile tf2=new TextFile(reference, false, false);
		Scaffold scaf=null;
		int len=0;
		final long[] acgtn=new long[8];
		for(String s=tf2.nextLine(); s!=null; s=tf2.nextLine()){
			if(s.startsWith(">")){
				if(scaf!=null){
					scaf.length=len;
					scaf.gc=(float)((acgtn[1]+acgtn[2])*1d/Data.max(1, acgtn[0]+acgtn[1]+acgtn[2]+acgtn[3]));
					scaf=null;
					len=0;
					Arrays.fill(acgtn, 0);
				}

				String name=s.substring(1);
				scaf=table.get(name);
				if(ADD_FROM_REF && scaf==null){
					scaf=new Scaffold(name, 0);
					System.err.println("Warning - SAM header did not include "+name);
					table.put(name, scaf);
				}
			}else{
				len+=s.length();
				for(int i=0; i<s.length(); i++){
					acgtn[charToNum[s.charAt(i)]]++;
				}
			}
		}
		if(scaf!=null){
			scaf.length=len;
			scaf.gc=(float)((acgtn[1]+acgtn[2])*1d/Data.max(1, acgtn[0]+acgtn[1]+acgtn[2]+acgtn[3]));
			scaf=null;
			len=0;
			Arrays.fill(acgtn, 0);
		}
	}
	
	
	public void processOrfsFasta(String fname_in, String fname_out, HashMap<String, Scaffold> map){
		TextFile tf=new TextFile(fname_in, false, false);
		assert(!fname_in.equalsIgnoreCase(fname_out));
		TextStreamWriter tsw=new TextStreamWriter(fname_out, overwrite, false, true);
		tsw.start();
		
		if(printHeader){
			String pound=(headerPound ? "#" : "");
			tsw.print(pound+"mappedBases="+mappedBases+"\n");
			tsw.print(pound+"mappedReads="+mappedReads+"\n");
			tsw.print(pound+"name\tlength\tdepthSum\tavgDepth\tavgDepth/mappedBases\tminDepth\tmaxDepth\tmedianDepth\tstdDevDepth\tfractionCovered\n");
		}
		
		String line;
		final StringBuilder sb=new StringBuilder(500);
//		Formatter formatter=new Formatter(sb);
		
		while((line=tf.nextLine())!=null){
			if(line.length()>1 && line.charAt(0)=='>'){
				
				String[] split=line.split(" # "); //' # ' used as delimiters
				
				String orfname=split[0].substring(1).trim(); //In case there are spaces around the ' # ' delimiters
				String scafname=orfname;
				if(scafname.contains("_")){//PRODIGAL pads _1 to the name of the first orf of a scaffold, and etc
					int last=scafname.lastIndexOf('_');
					boolean numeric=false;
					for(int i=last+1; i<scafname.length(); i++){
						if(Character.isDigit(scafname.charAt(i))){numeric=true;}
						else{numeric=false; break;}
					}
					if(numeric){scafname=scafname.substring(0, last);}
				}
				
				int start=Integer.parseInt(split[1].trim());
				int stop=Integer.parseInt(split[2].trim());
				int strand=Integer.parseInt(split[3].trim());
				if(strand==1){strand=Gene.PLUS;}else{strand=Gene.MINUS;}
				Orf orf=new Orf(orfname, start, stop, (byte)strand);
				
				Scaffold scaf=map.get(scafname);
//				if(scaf==null){scaf=map.get(orfname);}
				
//				assert(scaf!=null) : "\nCan't find scaffold for ("+orf+")\nfrom line\n"+line+"\n";
//				assert(orf.start>=0 && orf.stop<scaf.length) : "\norf goes out of scaffold bounds.\n"+orf+"\n"+scaf+"\n";
				
				if(scaf==null){
					System.err.println("Can't find scaffold for ("+orf+")\nfrom line\n"+line+"\nscafname='"+scafname+"'\norfname='"+orfname+"'");
					if(ABORT_ON_ERROR){
						tsw.poison();
						throw new RuntimeException("Aborting.");
					}
				}
				if(orf.start<0 && orf.stop>=scaf.length){
					Data.sysout.println("orf goes out of scaffold bounds.\n"+orf+"\n"+scaf);
					if(ABORT_ON_ERROR){
						tsw.poison();
						throw new RuntimeException("Aborting.");
					}
				}

				if(scaf!=null){
					CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1); //TODO:  Strand logic here depends on stranding protocol.
					orf.readCoverageArray(ca);
				}
				
//				{
//					tsw.print(String.format("%s\t%d\t", args));
//				}
				
				sb.append(orf.name).append('\t');
				sb.append(orf.length()).append('\t');
				sb.append(orf.baseDepth).append('\t');
				sb.append(String.format("%.4f", orf.avgCoverage())).append('\t');
				sb.append(orf.avgCoverage()/mappedBases);

				sb.append('\t');
				sb.append(orf.minDepth).append('\t');
				sb.append(orf.maxDepth).append('\t');
				sb.append(orf.medianDepth).append('\t');
				sb.append(String.format("%.4f",orf.stdevDepth)).append('\t');
				sb.append(String.format("%.4f",orf.fractionCovered()));

				sb.append('\n');
				tsw.print(sb.toString());
				sb.setLength(0);
			}
		}
		
		tsw.poison();
		tsw.waitForFinish();
	}
	
	
	/*--------------------------------------------------------------*/
	/*----------------         Inner Methods        ----------------*/
	/*--------------------------------------------------------------*/
	
	
	public boolean addCoverage(String scafName, byte[] seq, int start, int stop, int readlen, int strand){
		final Scaffold scaf=table.get(scafName);
		if(scaf==null){
			assert(false) : "Can't find "+scafName;
			return false;
		}
		start=Tools.max(start, 0);
		stop=Tools.min(stop, scaf.length-1);
		final int bases=stop-start+1;
		mappedBases+=readlen;
		mappedReads++;
		scaf.basehits+=bases;
		scaf.readhits++;
		if(strand==1){scaf.readhitsMinus++;}

		if(USE_COVERAGE_ARRAYS){
			if(scaf.obj1==null){
				scaf.obj1=(bits32 ? new CoverageArray3(table.size(), scaf.length+1) : new CoverageArray2(table.size(), scaf.length+1));
				if(STRANDED){
					scaf.obj2=(bits32 ? new CoverageArray3(table.size(), scaf.length+1) : new CoverageArray2(table.size(), scaf.length+1));
				}
			}
			CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
			if(START_ONLY){
				ca.increment(start);
			}else{
				ca.incrementRange(start, stop, 1);
			}
		}else if(USE_BITSETS){
			if(scaf.obj1==null){
				scaf.obj1=new BitSet(scaf.length+1);
				if(STRANDED){
					scaf.obj2=new BitSet(scaf.length+1);
				}
			}
			BitSet bs=(BitSet)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
			if(START_ONLY){
				bs.set(start);
			}else{
				bs.set(start, stop+1);
			}
		}
		
		if(seq!=null && scaf.basecount!=null){
			final long[] counts=scaf.basecount;
			for(int i=0; i<seq.length; i++){
				counts[charToNum[seq[i]]]++;
			}
		}
		return true;
	}
	
	
	public boolean processSamLine(String line){
		if(line==null || line.length()==0){
			return false;
		}else if(line.charAt(0)=='@'){
			if(!error){
				System.err.println("Unexpected header line: "+line);
				System.err.println("This should not cause problems, and is probably due to concatenated sam files.\n" +
						"Supressing future unexpected header warnings.");
				error=true;
			}
			
			if(line.startsWith("@SQ")){
				String[] split=line.split("\t");
				Scaffold scaf=new Scaffold(split);
				if(!table.containsKey(scaf.name)){
					if(COUNT_GC){scaf.basecount=new long[8];}
					table.put(scaf.name, scaf);
					list.add(scaf);
					refBases+=scaf.length;
				}
			}
		}else{
			SamLine sl=new SamLine(line);
			return processSamLine(sl);
		}
		return false;
	}
	
	
	public boolean processSamLine(SamLine sl){
		readsProcessed++;
		if(sl.mapped() && (USE_SECONDARY || sl.primary()) && (sl.seq!=null || sl.cigar!=null)){
			assert(sl.seq!=null) : sl.toString();
			int length=sl.seq==null ? SamLine.calcCigarLength(sl.cigar) : sl.seq.length;
			return addCoverage(new String(sl.rname()), sl.seq, sl.start(), sl.stop(), length, sl.strand());
		}
		return false;
	}
	
	
	public boolean processRead(Read r){
		readsProcessed++;
		if(r.mapped() && r.bases!=null){
			if(USE_SECONDARY && r.sites!=null && r.sites.size()>0){
				if(coords.set(r)){
					return addCoverage(new String(coords.name), r.bases, coords.start, coords.stop, r.length(), coords.strand);
				}
			}else{
				boolean b=false;
				for(SiteScore ss : r.sites){
					b=processRead(r, ss) || b;
				}
				return b;
			}
		}
		return false;
	}
	
	
	public boolean processRead(Read r, SiteScore ss){
		if(ss!=null && r.bases!=null){
			if(coords.set(ss)){
				return addCoverage(new String(coords.name), r.bases, coords.start, coords.stop, r.length(), coords.strand);
			}
		}
		return false;
	}

	
	/*--------------------------------------------------------------*/
	/*----------------        Output Methods        ----------------*/
	/*--------------------------------------------------------------*/
	
	
	public void printOutput(){
		
		totalScaffolds=list.size();
		
		String basecov1=(basecov==null ? null : (STRANDED ? basecov.replaceFirst("#", "1") : basecov));
		String bincov1=(bincov==null ? null : (STRANDED ? bincov.replaceFirst("#", "1") : bincov));
		String normcov1=(normcov==null ? null : (STRANDED ? normcov.replaceFirst("#", "1") : normcov));
		String normcovOverall1=(normcovOverall==null ? null : (STRANDED ? normcovOverall.replaceFirst("#", "1") : normcovOverall));
		String histogram1=(histogram==null ? null : (STRANDED ? histogram.replaceFirst("#", "1") : histogram));
		String stats1=(covstats==null ? null : (STRANDED ? covstats.replaceFirst("#", "1") : covstats));

		String basecov2=(basecov==null || !STRANDED ? null : basecov.replaceFirst("#", "2"));
		String bincov2=(bincov==null || !STRANDED ? null : bincov.replaceFirst("#", "2"));
		String normcov2=(normcov==null || !STRANDED ? null : normcov.replaceFirst("#", "2"));
		String normcovOverall2=(normcovOverall==null ? null : (STRANDED ? normcovOverall.replaceFirst("#", "2") : normcovOverall));
		String histogram2=(histogram==null || !STRANDED ? null : histogram.replaceFirst("#", "2"));
		String stats2=(covstats==null || !STRANDED ? null : covstats.replaceFirst("#", "2"));
		
		if(CONCISE){
			writeCoveragePerBaseConcise(basecov1, list, 0, minscaf);
			writeCoveragePerBaseConcise(basecov2, list, 1, minscaf);
		}else{
			writeCoveragePerBase(basecov1, list, DELTA_ONLY, 0, minscaf);
			writeCoveragePerBase(basecov2, list, DELTA_ONLY, 1, minscaf);
		}
		if(KEEP_SHORT_BINS){
			writeCoveragePerBaseBinned2(bincov1, list, binsize, 0, minscaf);
			writeCoveragePerBaseBinned2(bincov2, list, binsize, 1, minscaf);
		}else{
			writeCoveragePerBaseBinned(bincov1, list, binsize, 0, minscaf);
			writeCoveragePerBaseBinned(bincov2, list, binsize, 1, minscaf);
		}
		if(normcov!=null){
			writeCoveragePerBaseNormalized(normcov1, list, binsize, 0, minscaf);
			writeCoveragePerBaseNormalized(normcov2, list, binsize, 1, minscaf);
		}
		if(normcovOverall!=null){
			writeCoveragePerBaseNormalizedOverall(normcovOverall1, list, binsize, 0, minscaf);
			writeCoveragePerBaseNormalizedOverall(normcovOverall2, list, binsize, 1, minscaf);
		}
		
		
		{
			long[] hist=writeStats(stats1, 0);
			if(hist!=null){writeHist(histogram1, hist);}
			
			if(STRANDED){
				hist=writeStats(stats2, 1);
				if(hist!=null){writeHist(histogram2, hist);}
			}
		}
		

		double depthCovered=mappedBases*1.0/refBases;
		double pctScaffoldsWithCoverage=scaffoldsWithCoverage1*100.0/totalScaffolds;
		double pctCovered=totalCoveredBases1*100.0/refBases;
		
		Data.sysout.println(String.format("\nAverage coverage:                    \t%.2f", depthCovered));
		Data.sysout.println(String.format("Percent scaffolds with any coverage: \t%.2f", pctScaffoldsWithCoverage));
		if(USE_COVERAGE_ARRAYS || USE_BITSETS){
			Data.sysout.println(String.format("Percent of reference bases covered:  \t%.2f", pctCovered));
		}
	}
	
	
	public long[] writeStats(String fname, int strand){
//		System.err.println("Writing stats for "+fname+", "+strand);
		final TextStreamWriter tsw=(fname==null ? null : new TextStreamWriter(fname, overwrite, false, true));
		
		if(tsw!=null){
			tsw.start();
			if(printHeader){
				String pound=(headerPound ? "#" : "");
				if(TWOCOLUMN){
					tsw.println(pound+"ID\tAvg_fold");
				}else if(COUNT_GC){
					tsw.println(pound+
							"ID\tAvg_fold\tLength\tRef_GC\tCovered_percent\tCovered_bases\tPlus_reads\tMinus_reads"+(USE_COVERAGE_ARRAYS ? "\tMedian_fold" : "")+"\tRead_GC");
				}else{
					tsw.println(pound+
							"ID\tAvg_fold\tLength\tRef_GC\tCovered_percent\tCovered_bases\tPlus_reads\tMinus_reads"+(USE_COVERAGE_ARRAYS ? "\tMedian_fold" : ""));
				}
			}
		}
		
		final long[] hist=(USE_COVERAGE_ARRAYS ? new long[Character.MAX_VALUE+1] : null);
		final int histmax=Character.MAX_VALUE;
		
		long coveredScafTemp=0;
		long coveredBaseTemp=0;
		for(Scaffold scaf : list){
			final long sum=scaf.basehits;
			int covered=0;
			int median=-1;
			if(USE_COVERAGE_ARRAYS){
				CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
				if(ca!=null){
					for(int i=0; i<scaf.length; i++){
						int x=ca.get(i);
						hist[Tools.min(x, histmax)]++;
//						sum+=x;
						if(x>0){covered++;}
					}
					if(bits32){
						int[] array=((CoverageArray3)ca).array;
						Arrays.sort(array);
						Tools.reverseInPlace(array);
						median=ca.get(scaf.length/2);
					}else{
						char[] array=((CoverageArray2)ca).array;
						Arrays.sort(array);
						Tools.reverseInPlace(array);
						median=ca.get(scaf.length/2);
					}
				}
			}else if(USE_BITSETS){
//				sum+=scaf.basehits;
				BitSet bs=(BitSet)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
				covered=(bs==null ? 0 : bs.cardinality());
			}
			
			if(sum>0){
				coveredScafTemp++;
			}
			//			pw.print(scaf.name);
			if(tsw!=null && (sum>0 || !NONZERO_ONLY) && scaf.length>=minscaf){
				if(TWOCOLUMN){
					tsw.print(String.format("%s\t%.4f\n", scaf.name, sum/(double)scaf.length));
				}else if(COUNT_GC){
					long[] bc=scaf.basecount;
					double gc=(bc[1]+bc[2])*1d/Data.max(1, bc[0]+bc[1]+bc[2]+bc[3]);
					if(USE_COVERAGE_ARRAYS){
						tsw.print(String.format("%s\t%.4f\t%d\t%.4f\t%.4f\t%d\t%d\t%d\t%d\t%.4f\n", scaf.name, sum/(double)scaf.length, scaf.length,
								scaf.gc, covered*100d/scaf.length, covered, (scaf.readhits-scaf.readhitsMinus), scaf.readhitsMinus, median, gc));
					}else{
						tsw.print(String.format("%s\t%.4f\t%d\t%.4f\t%.4f\t%d\t%d\t%d\t%.4f\n", scaf.name, sum/(double)scaf.length, scaf.length,
								scaf.gc, covered*100d/scaf.length, covered, (scaf.readhits-scaf.readhitsMinus), scaf.readhitsMinus, gc/*, scaf.basehits*/));
					}
				}else{
					if(USE_COVERAGE_ARRAYS){
						tsw.print(String.format("%s\t%.4f\t%d\t%.4f\t%.4f\t%d\t%d\t%d\t%d\n", scaf.name, sum/(double)scaf.length, scaf.length,
								scaf.gc, covered*100d/scaf.length, covered, (scaf.readhits-scaf.readhitsMinus), scaf.readhitsMinus, median));
					}else{
						tsw.print(String.format("%s\t%.4f\t%d\t%.4f\t%.4f\t%d\t%d\t%d\n", scaf.name, sum/(double)scaf.length, scaf.length,
								scaf.gc, covered*100d/scaf.length, covered, (scaf.readhits-scaf.readhitsMinus), scaf.readhitsMinus));
					}
				}
			}
			coveredBaseTemp+=covered;
		}
		
		if(strand==0){
			scaffoldsWithCoverage1+=coveredScafTemp;
			totalCoveredBases1+=coveredBaseTemp;
		}else{
			scaffoldsWithCoverage2+=coveredScafTemp;
			totalCoveredBases2+=coveredBaseTemp;
		}
		
		if(tsw!=null){tsw.poisonAndWait();}
		return hist;
	}
	
	/**
	 * Write a histogram of number of bases covered to each depth
	 * @param fname Output filename
	 * @param counts counts[X] stores the number of bases with coverage X
	 */
	public static void writeHist(String fname, long[] counts){
		if(fname==null){return;}
		assert(counts!=null) : "Can't write a histogram with null counts.";
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		if(printHeader){
			String pound=(headerPound ? "#" : "");
			tsw.print(pound+"Coverage\tnumBases\n");
		}
		int max=0;
		for(max=counts.length-1; max>0 && counts[max]==0; max--){}
		for(int i=0; i<=max; i++){
			long x=counts[i];
			tsw.print(i+"\t"+x+"\n");
		}
		
		tsw.poisonAndWait();
	}
	
	/**
	 * Prints coverage in this format:
	 * scafname TAB position TAB coverage
	 * scafname TAB position TAB coverage
	 * @param fname Output filename
	 * @param list List of reference scaffolds
	 * @param deltaOnly Only write lines when coverage changes
	 * @param strand Only use coverage from reads mapped to this strand (0=plus, 1=minus)
	 */
	public static void writeCoveragePerBase(String fname, ArrayList<Scaffold> list, boolean deltaOnly, int strand, int minscaf){
		if(fname==null || (!STRANDED && strand>0)){return;}
		
		if(verbose){System.err.println("Starting tsw "+fname);}
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, true);
		if(verbose){System.err.println("Created tsw "+fname);}
		tsw.start();
//		if(verbose){System.err.println("Started tsw "+fname);}
		if(printHeader){
			String pound=(headerPound ? "#" : "");
			tsw.print(pound+"RefName\tPos\tCoverage\n");
		}
		
		for(Scaffold scaf : list){
			int last=-1;
			CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
			if(scaf.length>=minscaf){
				for(int i=0; i<scaf.length; i++){
					int x=(ca==null ? 0 : ca.get(i));
					if(!deltaOnly || x!=last){
						tsw.print(scaf.name+"\t"+(i+1)+"\t"+x+"\n");
						last=x;
					}
				}
			}
		}

		if(verbose){System.err.println("Closing tsw "+fname);}
		tsw.poisonAndWait();
		if(verbose){System.err.println("Closed tsw "+fname);}
	}
	
	/**
	 * Prints coverage in this format, skipping zero-coverage positions:
	 * #scafname
	 * position TAB coverage
	 * position TAB coverage 
	 * @param fname Output filename
	 * @param list List of reference scaffolds
	 * @param strand Only use coverage from reads mapped to this strand (0=plus, 1=minus)
	 */
	public static void writeCoveragePerBaseConcise(String fname, ArrayList<Scaffold> list, int strand, int minscaf){
		if(fname==null || (!STRANDED && strand>0)){return;}
		
		if(verbose){System.err.println("Starting tsw "+fname);}
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, true);
		tsw.start();
		if(verbose){System.err.println("Started tsw "+fname);}
//		tsw.print(pound+"RefName\tPos\tCoverage\n");
		
		for(Scaffold scaf : list){
			tsw.print("#"+scaf.name+"\n");
			CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
			if(scaf.length>=minscaf){
				for(int i=0; i<scaf.length; i++){
					int x=(ca==null ? 0 : ca.get(i));
					if(x>0){
						tsw.print(i+"\t"+x+"\n");
					}
				}
			}
		}

		if(verbose){System.err.println("Closing tsw "+fname);}
		tsw.poisonAndWait();
		if(verbose){System.err.println("Closed tsw "+fname);}
//		assert(false);
	}
	
	/**
	 * Note.  As written, this will truncate all trailing bases of each scaffold's length modulo binsize.
	 * For example, with binsize 1000, the last 500 bases of a 1500 base scaffold will be ignored.
	 * @param fname Output filename
	 * @param list List of reference scaffolds
	 * @param binsize Width of coverage bins in bp
	 * @param strand Only use coverage from reads mapped to this strand (0=plus, 1=minus)
	 */
	public static void writeCoveragePerBaseBinned(String fname, ArrayList<Scaffold> list, int binsize, int strand, int minscaf){
		if(fname==null || (!STRANDED && strand>0)){return;}
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		if(printHeader){
			String pound=(headerPound ? "#" : "");
			tsw.print(pound+"RefName\tCov\tPos\tRunningPos\n");
		}
		
		long running=0;
		final float invbin=1f/binsize;
		for(Scaffold scaf : list){
			if(scaf.length>=binsize && scaf.length>=minscaf){
				CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
				int lastPos=-1, nextPos=binsize-1;
				long sum=0;
				for(int i=0; i<scaf.length; i++){
					int x=(ca==null ? 0 : ca.get(i));
					sum+=x;
					if(i>=nextPos){
//						float bin=(i-lastPos);
//						tsw.print(String.format("%s\t%.2f\t%d\t%d\n", scaf.name, sum/bin, (i+1), running));
						tsw.print(String.format("%s\t%.2f\t%d\t%d\n", scaf.name, sum*invbin, (i+1), running));
						lastPos=i;
						running+=binsize;
						nextPos+=binsize;
						sum=0;
					}
				}
			}
		}
		
		tsw.poisonAndWait();
	}
	

	
	/**
	 * @param fname Output filename
	 * @param list List of reference scaffolds
	 * @param binsize Width of coverage bins in bp
	 * @param strand Only use coverage from reads mapped to this strand (0=plus, 1=minus)
	 */
	public static void writeCoveragePerBaseNormalized(String fname, ArrayList<Scaffold> list, double binsize, int strand, int minscaf){
		if(fname==null || (!STRANDED && strand>0)){return;}
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		if(printHeader){
			String pound=(headerPound ? "#" : "");
			tsw.print(pound+"RefName\tBin\tCov\tPos\tRunningPos\n");
		}
		
		double running=0;
		double invbin=1.0/binsize;
		final double invbincount=1.0/NORMALIZE_LENGTH_BINS;
		for(Scaffold scaf : list){
			if(NORMALIZE_LENGTH_BINS>0){
				binsize=scaf.length*invbincount;
				invbin=1.0/binsize;
			}
			
			if(scaf.length>=binsize && scaf.length>=minscaf){
				long max=-1;
				
				final CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
				double lastPos=-1, nextPos=binsize-1;
				long sum=0;

				if(NORMALIZE_COVERAGE){
					for(int i=0; i<scaf.length; i++){
						int x=(ca==null ? 0 : ca.get(i));
						sum+=x;
						if(i>=nextPos){
							max=Tools.max(sum, max);
							running+=binsize;
							nextPos+=binsize;
							sum=0;
						}
					}
					lastPos=-1;
					nextPos=binsize-1;
					sum=0;
					assert(max>-1) : max;
				}
				max=Tools.max(max, 1);
				final double binmult=(NORMALIZE_COVERAGE ? 1d/max : invbin);
				
//				assert(false) : NORMALIZE_COVERAGE+", "+binmult+", "+invbin+", "+max+", "+binsize;
				
				final String formatString=NORMALIZE_COVERAGE ? "%s\t%d\t%.5f\t%d\t%d\n" : "%s\t%d\t%.2f\t%d\t%d\n";
				int bin=1;
				for(int i=0; i<scaf.length; i++){
					int x=(ca==null ? 0 : ca.get(i));
					sum+=x;
					if(i>=nextPos){
//						System.err.println(x+", "+i+", "+nextPos+", "+sum+", "+(sum*binmult));
						tsw.print(String.format(formatString, scaf.name, bin, sum*binmult, (i+1), (long)running));
						bin++;
						lastPos=i;
						running+=binsize;
						nextPos+=binsize;
						sum=0;
					}
				}
			}
		}
		
		tsw.poisonAndWait();
	}
	

	
	/**
	 * @param fname Output filename
	 * @param list List of reference scaffolds
	 * @param binsize Width of coverage bins in bp
	 * @param strand Only use coverage from reads mapped to this strand (0=plus, 1=minus)
	 */
	public static void writeCoveragePerBaseNormalizedOverall(String fname, ArrayList<Scaffold> list, double binsize, int strand, int minscaf){
		if(fname==null || (!STRANDED && strand>0)){return;}
		
		assert(NORMALIZE_LENGTH_BINS>0) : "Must set 'normalizebins' flag to a positive integer.";
		double running=0;
		double invbin=1.0/binsize;
		long usedScafs=0;
		final double invbincount=1.0/NORMALIZE_LENGTH_BINS;

		double[] normalized=new double[NORMALIZE_LENGTH_BINS+1];
		double[] absolute=new double[NORMALIZE_LENGTH_BINS+1];
		
		for(Scaffold scaf : list){
			if(NORMALIZE_LENGTH_BINS>0){
				binsize=scaf.length*invbincount;
				invbin=1.0/binsize;
			}
			
			if(scaf.length>=binsize && scaf.length>=minscaf){
				usedScafs++;

				if(scaf.readhits>0){
					long max=-1;
					final CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
					double lastPos=-1, nextPos=binsize-1;
					long sum=0;

					{
						for(int i=0; i<scaf.length; i++){
							int x=(ca==null ? 0 : ca.get(i));
							sum+=x;
							if(i>=nextPos){
								max=Tools.max(sum, max);
								running+=binsize;
								nextPos+=binsize;
								sum=0;
							}
						}
						lastPos=-1;
						nextPos=binsize-1;
						sum=0;
						assert(max>-1) : max;
					}
					max=Tools.max(max, 1);
					final double binmult=1d/max;
					
					//				assert(false) : NORMALIZE_COVERAGE+", "+binmult+", "+invbin+", "+max+", "+binsize;

					int bin=1;
					for(int i=0; i<scaf.length; i++){
						int x=(ca==null ? 0 : ca.get(i));
						sum+=x;
						if(i>=nextPos){
							normalized[bin]+=(sum*binmult);
							absolute[bin]+=(sum*invbin);
							bin++;
							lastPos=i;
							running+=binsize;
							nextPos+=binsize;
							sum=0;
						}
					}
				}
			}
		}
		
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		if(printHeader){
			String pound=(headerPound ? "#" : "");
			tsw.print(pound+"RefName\tBin\tAbsCov\tNormCov\n");
		}
		double invScafs=1d/Tools.max(1, usedScafs);
		
		final double maxNorm=Tools.max(normalized);
		final double normMult=1/maxNorm;
		
		for(int bin=1; bin<normalized.length; bin++){
//			assert((absolute[bin]*invScafs)!=Double.NaN && (normalized[bin]*invScafs)!=Double.NaN) : invScafs+", "+absolute[bin]+", "+normalized[bin];
//			assert(false) : invScafs+", "+absolute[bin]+", "+normalized[bin]+", "+(absolute[bin]*invScafs)+", "+(normalized[bin]*invScafs);
			tsw.print(String.format("%s\t%d\t%.5f\t%.5f\n", "all", bin, absolute[bin]*invScafs, normalized[bin]*normMult));
		}
		
		tsw.poisonAndWait();
	}
	
	/**
	 * This version will NOT truncate all trailing bases of each scaffold's length modulo binsize.
	 * @param fname Output filename
	 * @param list List of reference scaffolds
	 * @param binsize Width of coverage bins in bp
	 * @param strand Only use coverage from reads mapped to this strand (0=plus, 1=minus)
	 */
	public static void writeCoveragePerBaseBinned2(String fname, ArrayList<Scaffold> list, int binsize, int strand, int minscaf){
		if(fname==null || (!STRANDED && strand>0)){return;}
		TextStreamWriter tsw=new TextStreamWriter(fname, overwrite, false, false);
		tsw.start();
		if(printHeader){
			String pound=(headerPound ? "#" : "");
			tsw.print(pound+"RefName\tCov\tPos\tRunningPos\n");
		}
		
		long running=0;
		for(Scaffold scaf : list){
			CoverageArray ca=(CoverageArray)(STRANDED && strand==1 ? scaf.obj2 : scaf.obj1);
			int lastPos=-1, nextPos=binsize-1;
			long sum=0;
			final int lim=scaf.length-1;
			for(int i=0; i<scaf.length; i++){
				int x=(ca==null ? 0 : ca.get(i));
				sum+=x;
				if(i>=nextPos || i==lim){
					int bin=(i-lastPos);
					if(scaf.length>=minscaf){
						tsw.print(String.format("%s\t%.2f\t%d\t%d\n", scaf.name, sum/(float)bin, (i+1), running));
					}
					running+=bin;
					nextPos+=binsize;
					lastPos=i;
					sum=0;
				}
			}
		}
		
		tsw.poisonAndWait();
	}
	
	/*--------------------------------------------------------------*/
	/*----------------            Fields            ----------------*/
	/*--------------------------------------------------------------*/
	
	/** The list of all scaffolds */
	private ArrayList<Scaffold> list;
	/** Maps names to scaffolds */
	private HashMap<String, Scaffold> table;
	/** Converts BBMap index coordinates to scaffold coordinates */
	private final ScaffoldCoordinates coords=new ScaffoldCoordinates();
	
	/** Mapping program name */
	private String program=null;
	/** Mapping program version */
	private String version=null;

	//Inputs
	/** Primary input sam file */
	public String in=null;
	/** Optional, for calculating GC */
	public String reference=null;
	public String orffasta=null;

	//Outputs
	/** Stream unaltered sam input to this output */
	public String outsam=null;
	/** Coverage statistics, one line per scaffold */
	public String covstats=null;
	public String outorf=null;
	/** Coverage histogram, one line per depth and one point per base */
	public String histogram=null;
	/** Coverage with one line per base */
	public String basecov=null;
	/** Coverage with one file per scaffold */
	public String basecov_ps=null;
	/** Coverage with one line per bin */
	public String bincov=null;
	/** Coverage with one line per bin, normalized by length and/or height */
	public String normcov=null;
	/** Coverage with one line per bin, normalized by length and/or height, for combined reference */
	public String normcovOverall=null;
	
	/** Typically indicates that a header line was encountered in an unexpected place, e.g. with concatenated sam files. */
	private boolean error=false;
	
	/** Total length of reference */
	public long refBases=0;
	public long mappedBases=0;
	public long mappedReads=0;
	public long readsProcessed=0;
	public long totalCoveredBases1=0;
	public long totalCoveredBases2=0;
	public long scaffoldsWithCoverage1=0;
	public long scaffoldsWithCoverage2=0;
	public long totalScaffolds=0;

	//Don't reset these variables when clearing.
	public long maxReads=-1;
	public int initialScaffolds=4096;
	public int binsize=1000;
	public boolean bits32=false;
	
	/** Don't print coverage info for scaffolds shorter than this */
	public int minscaf=0;
	
	/*--------------------------------------------------------------*/
	/*----------------        Static Fields         ----------------*/
	/*--------------------------------------------------------------*/
	

	/** Permission to overwrite existing files */
	public static boolean overwrite=false;
	/** Permission to append to existing files */
	public static boolean append=false;
	/** Print verbose log messages */
	public static boolean verbose=false;

	/** Print headers in output files */
	public static boolean printHeader=true;
	/** Prepend '#' symbol to header lines */
	public static boolean headerPound=true;
	
	/** Track base composition of reads covering each scaffold */
	public static boolean COUNT_GC=true;
	/** Output in 2-column format ("#ID\tAvg_fold\n") */
	public static boolean TWOCOLUMN=false;
	/** Track coverage for strands independently */
	public static boolean STRANDED=false;
	/** Add scaffold information from the reference (in addition to sam header) */
	public static boolean ADD_FROM_REF=false;
	/** Only print scaffolds with nonzero coverage */
	public static boolean NONZERO_ONLY=false;
	/** Store coverage info in numeric arrays */
	public static boolean USE_COVERAGE_ARRAYS=true;
	/** Store coverage info in bitsets */
	public static boolean USE_BITSETS=false;
	/** Only print lines when coverage changes (for compatibility with Jigsaw) */
	public static boolean DELTA_ONLY=false;
	/** Process secondary alignments */
	public static boolean USE_SECONDARY=true;
	/** Abort on error; otherwise, errors may be ignored */
	public static boolean ABORT_ON_ERROR=true;
	/** Print coverage for the last bin of a scaffold, even if it is shorter than binsize */
	public static boolean KEEP_SHORT_BINS=true;
	/** Only track coverage for start location */
	public static boolean START_ONLY=false;
	/** Only track coverage for start location */
	public static boolean CONCISE=false;
	/** Normalize coverage by expression contig coverage as a fraction of its max coverage */
	public static boolean NORMALIZE_COVERAGE=false;
	/** Normalize contig length by binning into this many bins per contig */
	public static int NORMALIZE_LENGTH_BINS=-1;

	/** Translation array for tracking base counts */
	private static final byte[] charToNum=AssemblyStats2.makeCharToNum();
	
}