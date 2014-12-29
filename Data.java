

package local.fjava.tick0;

// class to store int and block that int came from, for use in external mergesort
public class Data
    implements Comparable<Data>
{
  private int mData;

  private int mBlock;

  public Data(int data, int block)
  {
    mData = data;
    mBlock = block;
  }

  public int getData()
  {
    return mData;
  }

  public int getBlock()
  {
    return mBlock;
  }

  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + mData;
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Data other = (Data) obj;
    if (mData != other.mData)
      return false;
    return true;
  }

  @Override
  public String toString()
  {
    return Integer.toString(mData);
  }

  @Override
  public int compareTo(Data d)
  {
    return Integer.compare(this.mData, d.mData);
    // return this.mData - d.mData;
    // this caused problems, unsure why
  }
}
