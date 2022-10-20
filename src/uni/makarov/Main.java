package uni.makarov;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

//Варіант 11:
//Реалізувати алгоритм мінімізації Дет. Ск. автомата
public class Main {

    public static void main(String[] args) throws FileNotFoundException {
        new Main();
    }

    public Main() throws FileNotFoundException {
        Scanner inFile = new Scanner(new File("text.txt"));
        DFA automaton = readDFA(inFile);
        automaton.trim();
        automaton.minimize();
        System.out.println(automaton.toString());
    }

    private DFA readDFA(Scanner inFile) {
        int numAlpha = inFile.nextInt();
        int numStates = inFile.nextInt();
        int startState = inFile.nextInt();

        int numAccept = inFile.nextInt();
        HashSet<Integer> acceptStates = new HashSet<>(numAccept);
        for (int i = 0; i < numAccept; ++i) {
            acceptStates.add(inFile.nextInt());
        }

        DFAState[] states = new DFAState[numStates];
        while (inFile.hasNext()){
            int outState = inFile.nextInt();
            Character letter = inFile.next().charAt(0);
            int inState = inFile.nextInt();

            if(states[outState] != null) {
                DFAState state = states[outState];
                state.transitions.add(inState);
                state.alphabetTransitions.put(letter, inState);
            }
            else {
                ArrayList<Integer> transitions = new ArrayList<Integer>();
                HashMap alphabetTransitions = new HashMap<Character, Integer>();

                transitions.add(inState);
                alphabetTransitions.put(letter, inState);
                DFAState state = new DFAState(transitions, alphabetTransitions);
                states[outState] = state;
            }
        }

        return new DFA(numAlpha, startState, acceptStates, states);
    }

    private class DFA {
        char[] alphabet;
        DFAState[] states;
        Integer startState;
        HashSet<Integer> acceptStates;

        //Змінні для алгоритму
        private boolean[][] D;
        private ArrayList<ArrayList<HashSet<Point>>> S;

        public DFA(int numAlphabet, int startState, HashSet<Integer> acceptStates, DFAState[] states) {
            alphabet = new char[numAlphabet];
            this.states = states;
            this.startState = startState;
            this.acceptStates = acceptStates;

            // fill alphabet
            for (int i = 0; i < numAlphabet; i++) {
                alphabet[i] = (char) (i + 97);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Formatter formatter = new Formatter(sb, Locale.US);

            formatter.format("%d\n", alphabet.length);
            formatter.format("%d\n", states.length);

            // accept states
            ArrayList<Integer> acceptable = new ArrayList<Integer>(acceptStates);
            Collections.sort(acceptable);
            formatter.format("%d ", acceptable.size());
            for (int i = 0; i < acceptable.size(); i++) {
                Integer val = acceptable.get(i);
                if (i < acceptable.size() - 1) {
                    formatter.format("%d ", val);
                } else {
                    formatter.format("%d\n", val);
                }
            }

            // states
            for(int i = 0; i < states.length; i++) {
                DFAState state = states[i];
                for(char letter : alphabet){
                    formatter.format("%d %s %d\n", i, letter, state.alphabetTransitions.get(letter));
                }
            }

            return sb.toString();
        }

        public void minimize() {

            D = new boolean[states.length][states.length];
            S = new ArrayList<ArrayList<HashSet<Point>>>();  // lol

            for (int i = 0; i < states.length; i++) {
                ArrayList<HashSet<Point>> innerList = new ArrayList<HashSet<Point>>();

                for (int j = 0; j < states.length; j++) {
                    Arrays.fill(D[i], false);
                    innerList.add(new HashSet<Point>());
                }
                S.add(innerList);
            }

            for (int i = 0; i < states.length; i++) {
                for (int j = i + 1; j < states.length; j++) {
                    if (acceptStates.contains(i) != acceptStates.contains(j)) {
                        D[i][j] = true;
                    }
                }
            }

            for (int i = 0; i < states.length; i++) {
                for (int j = i + 1; j < states.length; j++) {
                    // only pairs that are as of yet indistinguishable
                    if (D[i][j]) {
                        continue;
                    }

                    DFAState qi = states[i];
                    DFAState qj = states[j];

                    // one of the things being compared is unreachable
                    if (qi == null || qj == null) {
                        continue;
                    }

                    // helps emulate "for any"
                    boolean distinguished = false;
                    for (int k = 0; k < qi.transitions.size(); k++) {
                        int m = qi.transitions.get(k);
                        int n = qj.transitions.get(k);

                        // if on the same letter, qm and qn move to distinguishable states
                        if (D[m][n] || D[n][m]) {
                            dist(i, j);
                            distinguished = true;
                            break;
                        }
                    }

                    if (!distinguished) {
                        // qm and qn are indistinguishable
                        for (int k = 0; k < qi.transitions.size(); k++) {
                            int m = qi.transitions.get(k);
                            int n = qj.transitions.get(k);

                            if (m < n && !(i == m && j == n)) {
                                S.get(m).get(n).add(new Point(i, j));
                            } else if (m > n && !(i == n && j == m)) {
                                S.get(n).get(m).add(new Point(i, j));
                            }
                        }
                    }

                }
            }

            mergeStates();
        }

        private void mergeStates() {
            ArrayList<DFAState> newStates = new ArrayList<DFAState>();
            HashSet<Integer> newAcceptStates = new HashSet<Integer>();
            HashMap<Integer, Integer> merged = new HashMap<Integer, Integer>();
            ArrayList<ArrayList<Integer>> mergeGroups = new ArrayList<ArrayList<Integer>>();
            for (int i = 0; i < D.length; i++) {
                if (merged.get(i) != null || states[i] == null) {
                    continue;
                }

                DFAState state = states[i];

                ArrayList<Integer> toMerge = new ArrayList<Integer>();
                for (int j = i + 1; j < D.length; j++) {
                    if (!D[i][j]) {
                        toMerge.add(j);
                        merged.put(j, i);
                    }
                }

                for (int j = 0; j < state.transitions.size(); j++) {
                    Integer transition = state.transitions.get(j);
                    if (merged.containsKey(transition)) {
                        state.transitions.set(j, merged.get(transition));
                    }
                }

                if (acceptStates.contains(i)) {
                    newAcceptStates.add(i);
                }
                toMerge.add(i);
                mergeGroups.add(toMerge);
                newStates.add(state);
            }

            renumberStates(mergeGroups, newAcceptStates);

            DFAState[] newStatesArray = new DFAState[newStates.size()];
            newStatesArray = newStates.toArray(newStatesArray);
            states = newStatesArray;
            acceptStates = newAcceptStates;
        }

        private void renumberStates(ArrayList<ArrayList<Integer>> groups, HashSet<Integer> newAcceptStates) {
            for (int i = 0; i < groups.size(); i++) {
                ArrayList<Integer> group = groups.get(i);
                for (DFAState state : states) {
                    if (state == null) {
                        continue;
                    }
                    for (int j = 0; j < state.transitions.size(); j++) {
                        Integer val = state.transitions.get(j);
                        if (group.contains(val)) {
                            state.transitions.set(j, i);
                        }
                    }
                }
                for (Integer state : new HashSet<Integer>(newAcceptStates)) {
                    if (group.contains(state)) {
                        newAcceptStates.remove(state);
                        newAcceptStates.add(i);
                    }
                }
            }
            for (DFAState state : states){
                if(state == null){
                    continue;
                }
                else state.syncTransitions();
            }
        }

        private void dist(int i, int j) {
            _dist(new Point(i, j), new HashSet<Point>());
        }

        private void _dist(Point point, HashSet<Point> visited) {
            if (visited.contains(point)) {
                return;
            }

            int i = point.x, j = point.y;
            D[i][j] = true;
            visited.add(point);
            for (Point pair : S.get(i).get(j)) {
                _dist(pair, visited);
            }
        }

        public void trim() {
            boolean[] visited = new boolean[states.length];
            Queue<Integer> visitQueue = new LinkedList<Integer>();

            // init with q0
            visitQueue.add(0);
            visited[0] = true;
            while (!visitQueue.isEmpty()) {
                int toVisit = visitQueue.remove();
                DFAState visitingState = states[toVisit];
                for (int otherState : visitingState.transitions) {
                    if (!visited[otherState]) {
                        visitQueue.add(otherState);
                    }
                }
                visited[toVisit] = true;
            }

            // null out unreachable states
            for (int i = 0; i < visited.length; i++) {
                if (!visited[i]) {
                    states[i] = null;
                }
            }
        }
    }

    private class DFAState {
        public ArrayList<Integer> transitions;
        public HashMap<Character, Integer> alphabetTransitions;

        public DFAState(ArrayList<Integer> transitions, HashMap<Character, Integer> alphabetTransitions) {
            this.transitions = transitions;
            this.alphabetTransitions = alphabetTransitions;
        }

        //only working with arraylist transitions because immutable types are hell
        public void syncTransitions(){
            int i = 0;
            for (char key : alphabetTransitions.keySet()){
                alphabetTransitions.put(key, transitions.get(i));
                i++;
            }
        }
    }
}
