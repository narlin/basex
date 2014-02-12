package org.basex.query.value.seq;

import java.util.*;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.type.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * Sequence of items of type {@link Str xs:string}, containing at least two of them.
 *
 * @author BaseX Team 2005-13, BSD License
 * @author Christian Gruen
 */
public final class StrSeq extends NativeSeq {
  /** Values. */
  private final byte[][] values;

  /**
   * Constructor.
   * @param vals values
   */
  private StrSeq(final byte[][] vals) {
    super(vals.length, AtomType.STR);
    values = vals;
  }

  @Override
  public Str itemAt(final long pos) {
    return Str.get(values[(int) pos]);
  }

  @Override
  public boolean sameAs(final Expr cmp) {
    return cmp instanceof StrSeq && Arrays.equals(values, ((StrSeq) cmp).values);
  }

  @Override
  public String[] toJava() {
    final String[] tmp = new String[values.length];
    for(int v = 0; v < values.length; v++) tmp[v] = Token.string(values[v]);
    return tmp;
  }

  @Override
  public Value reverse() {
    final int s = values.length;
    final byte[][] tmp = new byte[s][];
    for(int l = 0, r = s - 1; l < s; l++, r--) tmp[l] = values[r];
    return get(tmp);
  }

  // STATIC METHODS =====================================================================

  /**
   * Creates a sequence with the specified items.
   * @param items items
   * @return value
   */
  public static Value get(final TokenList items) {
    return items.isEmpty() ? Empty.SEQ : items.size() == 1 ?
        Str.get(items.get(0)) : new StrSeq(items.toArray());
  }

  /**
   * Creates a sequence with the specified items.
   * @param items items
   * @return value
   */
  public static Value get(final byte[][] items) {
    return items.length == 0 ? Empty.SEQ : items.length == 1 ?
        Str.get(items[0]) : new StrSeq(items);
  }

  /**
   * Creates a sequence with the items in the specified expressions.
   * @param vals values
   * @param size size of resulting sequence
   * @return value
   * @throws QueryException query exception
   */
  public static Value get(final Value[] vals, final int size) throws QueryException {
    final byte[][] tmp = new byte[size][];
    int t = 0;
    for(final Value val : vals) {
      // speed up construction, depending on input
      final int vs = (int) val.size();
      if(val instanceof Item) {
        tmp[t++] = ((Item) val).string(null);
      } else if(val instanceof StrSeq) {
        final StrSeq sq = (StrSeq) val;
        System.arraycopy(sq.values, 0, tmp, t, vs);
        t += vs;
      } else {
        for(int v = 0; v < vs; v++) tmp[t++] = val.itemAt(v).string(null);
      }
    }
    return get(tmp);
  }
}
