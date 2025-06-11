package io.aeron.samples.admin.model;

public class Pair<S,T>
{
    private final S first;
    private final T second;

    public Pair(S first, T second)
    {
        this.first = first;
        this.second = second;
    }

    public S getFirst()
    {
        return first;
    }

    public T getSecond()
    {
        return second;
    }

    @Override
    public String toString()
    {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
