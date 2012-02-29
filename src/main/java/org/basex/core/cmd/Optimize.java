package org.basex.core.cmd;

import static org.basex.core.Text.*;

import java.io.IOException;

import org.basex.core.User;
import org.basex.data.Data;
import org.basex.data.MetaData;
import org.basex.index.IndexToken.IndexType;
import org.basex.util.Util;
import org.basex.util.list.IntList;

/**
 * Evaluates the 'optimize' command and optimizes the data structures of
 * the currently opened database. Indexes and statistics are refreshed,
 * which is especially helpful after updates.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Christian Gruen
 */
public final class Optimize extends ACreate {
  /** Current pre value. */
  private int pre;
  /** Data size. */
  private int size;

  /**
   * Default constructor.
   */
  public Optimize() {
    super(DATAREF | User.WRITE);
  }

  @Override
  protected boolean run() {
    final Data d = context.data();
    final MetaData m = d.meta;
    size = m.size;

    try {
      optimize(d, this);
    } catch(final IOException ex) {
      Util.debug(ex);
    }
    return info(DB_OPTIMIZED_X, m.name, perf);
  }

  @Override
  public double prog() {
    return (double) pre / size;
  }

  @Override
  public boolean stoppable() {
    return false;
  }

  @Override
  public String det() {
    return CREATE_STATS_D;
  }

  /**
   * Optimize data structures.
   * @param d data
   * @throws IOException I/O Exception during index rebuild
   */
  public static void optimize(final Data d) throws IOException {
    optimize(d, null);
  }

  /**
   * Optimize data structures.
   * @param data data
   * @param c calling command (can be null)
   * @throws IOException I/O Exception during index rebuild
   */
  private static void optimize(final Data data, final Optimize c)
      throws IOException {

    // initialize structural indexes
    final MetaData md = data.meta;
    if(!md.uptodate) {
      data.paths.close();
      data.resources.init();
      data.tagindex.init();
      data.atnindex.init();
      md.dirty = true;

      final IntList pars = new IntList();
      final IntList tags = new IntList();
      int n = 0;

      for(int pre = 0; pre < md.size; ++pre) {
        final byte kind = (byte) data.kind(pre);
        final int par = data.parent(pre, kind);
        while(!pars.isEmpty() && pars.peek() > par) {
          pars.pop();
          tags.pop();
        }
        final int level = pars.size();
        if(kind == Data.DOC) {
          data.paths.index(0, kind, level);
          pars.push(pre);
          tags.push(0);
          ++n;
        } else if(kind == Data.ELEM) {
          final int id = data.name(pre);
          data.tagindex.index(data.tagindex.key(id), null, true);
          data.paths.index(id, kind, level);
          pars.push(pre);
          tags.push(id);
        } else if(kind == Data.ATTR) {
          final int id = data.name(pre);
          final byte[] val = data.text(pre, false);
          data.atnindex.index(data.atnindex.key(id), val, true);
          data.paths.index(id, kind, level, val, md);
        } else {
          final byte[] val = data.text(pre, true);
          if(kind == Data.TEXT && level > 1) data.tagindex.index(tags.peek(), val);
          data.paths.index(0, kind, level, val, md);
        }
        if(c != null) c.pre = pre;
      }
      md.ndocs = n;
      md.uptodate = true;
    }

    try {
      // rebuild value indexes
      optimize(IndexType.ATTRIBUTE, data, md.createattr, md.attrindex, c);
      optimize(IndexType.TEXT,      data, md.createtext, md.textindex, c);
      optimize(IndexType.FULLTEXT,  data, md.createftxt, md.ftxtindex, c);
    } finally {
      data.flush();
    }
  }

  /**
   * Optimizes the specified index.
   * @param type index type
   * @param d data reference
   * @param create create flag
   * @param old old flag
   * @param c calling command
   * @throws IOException I/O exception
   *
   */
  private static void optimize(final IndexType type, final Data d,
      final boolean create, final boolean old, final Optimize c) throws IOException {

    // check if flags are already consistent
    if(create == old) return;
    // create or drop index
    if(create) create(type, d, c);
    else drop(type, d);
  }
}
